package org.com.drop.domain.auction.list;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Set;

import org.com.drop.BaseIntegrationTest;
import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.auction.entity.Auction.AuctionStatus;
import org.com.drop.domain.auction.auction.repository.AuctionRepository;
import org.com.drop.domain.auction.bid.entity.Bid;
import org.com.drop.domain.auction.bid.repository.BidRepository;
import org.com.drop.domain.auction.list.service.BookmarkCacheService;
import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.auction.product.entity.ProductImage;
import org.com.drop.domain.auction.product.repository.ProductImageRepository;
import org.com.drop.domain.auction.product.repository.ProductRepository;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.com.drop.global.aws.AmazonS3Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Transactional
@DisplayName("경매 컨트롤러 통합 테스트 (북마크 Redis 캐싱 적용)")
class AuctionControllerIntegrationTest extends BaseIntegrationTest {

	@Autowired MockMvc mockMvc;
	@Autowired ObjectMapper objectMapper;
	@Autowired UserRepository userRepository;
	@Autowired ProductRepository productRepository;
	@Autowired ProductImageRepository productImageRepository;
	@Autowired AuctionRepository auctionRepository;
	@Autowired BidRepository bidRepository;
	@Autowired CacheManager cacheManager;

	/**
	 * AmazonS3Client를 Mock으로 대체.
	 */
	@MockitoBean
	private AmazonS3Client amazonS3Client;

	/**
	 * BookmarkCacheService를 Mock으로 대체.
	 */
	@MockitoBean
	private BookmarkCacheService bookmarkCacheService;

	private User seller;
	private User buyer;
	private Auction liveAuction;
	private Auction endingSoonAuction;
	private Auction endedAuction;
	private Product product1;
	private Product product2;
	private Product product3;

	@BeforeEach
	void setUp() {
		// 캐시 초기화
		cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());

		// 1. 유저 생성
		seller = createUser("seller@test.com", "판매자");
		buyer = createUser("buyer@test.com", "구매자");

		// 2. 상품 생성
		product1 = createProduct(seller, "한정판 아이돌 굿즈", Product.Category.STARGOODS);
		product2 = createProduct(seller, "희귀 피규어", Product.Category.FIGURE);
		product3 = createProduct(seller, "마감임박 상품", Product.Category.STARGOODS);

		// 3. 경매 생성
		liveAuction = createAuction(product1, AuctionStatus.LIVE, LocalDateTime.now().plusHours(24));
		endingSoonAuction = createAuction(product3, AuctionStatus.LIVE, LocalDateTime.now().plusHours(3)); // 3시간 후 마감
		endedAuction = createAuction(product2, AuctionStatus.ENDED, LocalDateTime.now().minusDays(1));

		// 4. 입찰 생성 (종료된 경매에 입찰 내역 추가)
		createBid(endedAuction, buyer, 1200000L);
		createBid(endedAuction, seller, 1100000L);
		createBid(endedAuction, buyer, 1300000L);

		// 인기 경매를 위해 입찰 추가
		createBid(liveAuction, buyer, 1100000L);
		createBid(liveAuction, seller, 1200000L);

		// 5. AmazonS3Client Mock 설정
		given(amazonS3Client.getPresignedUrl(anyString()))
			.willAnswer(invocation -> "https://s3.amazonaws.com/test-bucket/" + invocation.getArgument(0));

		// 6. BookmarkCacheService Mock 설정 - 기본값
		// 로그인하지 않은 사용자는 null 반환 (캐시 미스)
		given(bookmarkCacheService.getBookmarkedProductIds(isNull()))
			.willReturn(null);

		// 특정 사용자의 북마크 목록은 기본적으로 빈 Set 반환
		given(bookmarkCacheService.getBookmarkedProductIds(anyLong()))
			.willReturn(Set.of());

		// cacheUserBookmarks는 아무 동작도 하지 않음
		willDoNothing().given(bookmarkCacheService).cacheUserBookmarks(anyLong(), anyList());
	}

	private User createUser(String email, String nickname) {
		return userRepository.save(User.builder()
			.email(email)
			.nickname(nickname)
			.password("password")
			.loginType(User.LoginType.LOCAL)
			.role(User.UserRole.USER)
			.build());
	}

	private Product createProduct(User seller, String name, Product.Category category) {
		Product product = productRepository.save(Product.builder()
			.seller(seller)
			.name(name)
			.description("테스트 상품 설명")
			.category(category)
			.subcategory(Product.SubCategory.ETC)
			.createdAt(LocalDateTime.now())
			.bookmarkCount(0)
			.build());
		productImageRepository.save(new ProductImage(product, "test-image.jpg"));
		return product;
	}

	private Auction createAuction(Product product, AuctionStatus status, LocalDateTime endAt) {
		return auctionRepository.save(Auction.builder()
			.product(product)
			.startPrice(1000000)
			.buyNowPrice(2000000)
			.minBidStep(50000)
			.startAt(LocalDateTime.now().minusHours(1))
			.endAt(endAt)
			.status(status)
			.bidCount(0)
			.build());
	}

	// ================= Helper Methods =================

	private void createBid(Auction auction, User bidder, Long amount) {
		bidRepository.save(Bid.builder()
			.auction(auction)
			.bidder(bidder)
			.bidAmount(amount)
			.createdAt(LocalDateTime.now())
			.isAuto(false)
			.build());
		auction.increaseBidCount();
	}

	@Nested
	@DisplayName("경매 목록 조회")
	class GetAuctions {
		// ... (기존 테스트 메서드들 유지)
	}

	@Nested
	@DisplayName("경매 상세 조회")
	class GetAuctionDetail {
		// ... (기존 테스트 메서드들 유지)
	}

	@Nested
	@DisplayName("입찰 및 최고가 조회")
	class BidAndHistory {
		// ... (기존 테스트 메서드들 유지)

		@Nested
		@DisplayName("홈 화면 조회 API")
		class Home {

			@Test
			@DisplayName("성공: 홈 화면 데이터(마감 임박, 인기 경매)를 조회해야 한다")
			void getHomeData_success() throws Exception {
				// 캐시 초기화
				cacheManager.getCache("homeAuctions").clear();

				ResultActions resultActions = mockMvc.perform(get("/api/v1/auctions/home"))
					.andDo(print());

				resultActions
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.endingSoon").isArray())
					.andExpect(jsonPath("$.data.endingSoon").isNotEmpty()) // 비어있지 않아야 함
					.andExpect(jsonPath("$.data.popular").isArray())
					.andExpect(jsonPath("$.data.popular").isNotEmpty()) // 비어있지 않아야 함
					.andExpect(jsonPath("$.data.endingSoon[0].remainingTimeSeconds").isNumber())
					.andExpect(jsonPath("$.data.popular[0].bidCount").isNumber());
			}

			@Test
			@WithMockUser(username = "buyer@test.com")
			@DisplayName("성공: 로그인한 사용자의 홈 화면 조회 (Redis 캐싱 적용)")
			void getHomeData_withLogin_success() throws Exception {
				// 캐시 초기화
				cacheManager.getCache("homeAuctions").clear();

				// given: buyer의 북마크 목록 설정 - product1과 product3을 북마크
				given(bookmarkCacheService.getBookmarkedProductIds(buyer.getId()))
					.willReturn(Set.of(product1.getId(), product3.getId()));

				ResultActions resultActions = mockMvc.perform(get("/api/v1/auctions/home"))
					.andDo(print());

				resultActions
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.endingSoon").isArray())
					.andExpect(jsonPath("$.data.endingSoon").isNotEmpty())
					.andExpect(jsonPath("$.data.popular").isArray())
					.andExpect(jsonPath("$.data.popular").isNotEmpty())
					.andExpect(jsonPath("$.data.endingSoon[0].isBookmarked").isBoolean())
					.andExpect(jsonPath("$.data.popular[0].isBookmarked").isBoolean());

				// 북마크가 적용되었는지 확인
				String responseBody = resultActions.andReturn().getResponse().getContentAsString();
				JsonNode jsonNode = objectMapper.readTree(responseBody);

				JsonNode endingSoonItems = jsonNode.path("data").path("endingSoon");
				JsonNode popularItems = jsonNode.path("data").path("popular");

				if (endingSoonItems.size() > 0) {
					// product3 (마감임박 상품)은 북마크됨
					assert endingSoonItems.get(0).path("isBookmarked").asBoolean() == true;
				}

				if (popularItems.size() > 0) {
					// product1 (한정판 아이돌 굿즈)은 북마크됨
					assert popularItems.get(0).path("isBookmarked").asBoolean() == true;
				}
			}

			@Test
			@WithMockUser(username = "buyer@test.com")
			@DisplayName("성공: 홈 화면 캐싱 동작 확인")
			void getHomeData_caching_behavior() throws Exception {
				// 캐시 초기화
				cacheManager.getCache("homeAuctions").clear();

				// 첫 번째 요청 - 캐시 미스 (DB 조회)
				ResultActions firstRequest = mockMvc.perform(get("/api/v1/auctions/home"))
					.andDo(print());

				firstRequest.andExpect(status().isOk());

				// 두 번째 요청 - 캐시 히트 (캐시에서 조회)
				ResultActions secondRequest = mockMvc.perform(get("/api/v1/auctions/home"))
					.andDo(print());

				secondRequest.andExpect(status().isOk());

				// BookmarkCacheService가 호출되었는지 확인
				then(bookmarkCacheService).should(atLeastOnce()).getBookmarkedProductIds(buyer.getId());
			}

			@Test
			@DisplayName("성공: 로그인하지 않은 사용자의 홈 화면 조회")
			void getHomeData_anonymous_success() throws Exception {
				// 캐시 초기화
				cacheManager.getCache("homeAuctions").clear();

				// given: 익명 사용자는 북마크 없음
				given(bookmarkCacheService.getBookmarkedProductIds(isNull()))
					.willReturn(null);

				ResultActions resultActions = mockMvc.perform(get("/api/v1/auctions/home"))
					.andDo(print());

				resultActions
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.endingSoon").isArray())
					.andExpect(jsonPath("$.data.endingSoon").isNotEmpty())
					.andExpect(jsonPath("$.data.popular").isArray())
					.andExpect(jsonPath("$.data.popular").isNotEmpty())
					.andExpect(jsonPath("$.data.endingSoon[0].isBookmarked").value(false)) // 익명 사용자는 북마크 없음
					.andExpect(jsonPath("$.data.popular[0].isBookmarked").value(false)); // 익명 사용자는 북마크 없음
			}
		}
	}
}
