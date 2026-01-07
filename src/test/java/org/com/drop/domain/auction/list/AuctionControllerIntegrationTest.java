package org.com.drop.domain.auction.list;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Collections;
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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Transactional
@ActiveProfiles("test")
@DisplayName("경매 컨트롤러 통합 테스트 (북마크 Redis 캐싱 적용)")
class AuctionControllerIntegrationTest extends BaseIntegrationTest {

	@Autowired
	MockMvc mockMvc;
	@Autowired
	ObjectMapper objectMapper;
	@Autowired
	UserRepository userRepository;
	@Autowired
	ProductRepository productRepository;
	@Autowired
	ProductImageRepository productImageRepository;
	@Autowired
	AuctionRepository auctionRepository;
	@Autowired
	BidRepository bidRepository;
	@Autowired
	CacheManager cacheManager;

	/**
	 * AmazonS3Client를 Mock으로 대체하여 외부 통신 차단
	 */
	@MockitoBean
	private AmazonS3Client amazonS3Client;

	/**
	 * [핵심] BookmarkCacheService를 Mock으로 대체.
	 * 실제 Redis(RedissonClient)에 접속하지 않도록 하여 NPE 에러를 방지합니다.
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
		// Spring Cache (@Cacheable 사용 부분) 초기화
		if (cacheManager != null) {
			cacheManager.getCacheNames().forEach(name -> {
				Cache cache = cacheManager.getCache(name);
				if (cache != null) {
					cache.clear();
				}
			});
		}

		// 1. 유저 생성
		seller = createUser("seller@test.com", "판매자");
		buyer = createUser("buyer@test.com", "구매자");

		// 2. 상품 생성
		product1 = createProduct(seller, "한정판 아이돌 굿즈", Product.Category.STARGOODS);
		product2 = createProduct(seller, "희귀 피규어", Product.Category.FIGURE);
		product3 = createProduct(seller, "마감임박 상품", Product.Category.STARGOODS);

		// 3. 경매 생성
		liveAuction = createAuction(
			product1, AuctionStatus.LIVE, LocalDateTime.now().plusHours(24));
		endingSoonAuction = createAuction(
			product3, AuctionStatus.LIVE, LocalDateTime.now().plusHours(3)); // 3시간 후 마감
		endedAuction = createAuction(
			product2, AuctionStatus.ENDED, LocalDateTime.now().minusDays(1));

		// 4. 입찰 생성
		createBid(endedAuction, buyer, 1200000L);
		createBid(endedAuction, seller, 1100000L);
		createBid(endedAuction, buyer, 1300000L);

		createBid(liveAuction, buyer, 1100000L);
		createBid(liveAuction, seller, 1200000L);

		// 5. AmazonS3Client Mock 설정
		given(amazonS3Client.getPresignedUrl(anyString()))
			.willAnswer(invocation -> "https://s3.amazonaws.com/test-bucket/" + invocation.getArgument(0));

		// 6. BookmarkCacheService Mock 기본 동작 설정 (NPE 방지 및 기본 로직)
		// 비로그인 사용자(null) 요청 시 null 반환
		given(bookmarkCacheService.getBookmarkedProductIds(isNull()))
			.willReturn(null);

		// 특정 ID 요청 시 기본적으로 빈 Set 반환 (테스트마다 덮어씌움)
		given(bookmarkCacheService.getBookmarkedProductIds(anyLong()))
			.willReturn(Collections.emptySet());

		// 캐시 저장 메서드는 아무 동작도 하지 않음 (void)
		willDoNothing().given(bookmarkCacheService).cacheUserBookmarks(anyLong(), anyList());

		// 캐시 삭제/무효화 메서드도 아무 동작 하지 않음
		willDoNothing().given(bookmarkCacheService).addBookmark(anyLong(), anyLong());
		willDoNothing().given(bookmarkCacheService).removeBookmark(anyLong(), anyLong());
		willDoNothing().given(bookmarkCacheService).invalidateUserBookmarkCache(anyLong());
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

		@Test
		@WithMockUser(username = "buyer@test.com")
		@DisplayName("기본 조회 - LIVE 상태 확인 및 북마크 Redis 캐싱 적용")
		void t1() throws Exception {
			// given: Redis에서 해당 유저가 product1을 북마크하고 있다고 Mocking
			given(bookmarkCacheService.getBookmarkedProductIds(buyer.getId()))
				.willReturn(Set.of(product1.getId()));

			ResultActions resultActions = mockMvc.perform(
				get("/api/v1/auctions")
					.param("status", "LIVE")
					.param("size", "10")
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			resultActions
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items").isArray())
				.andExpect(jsonPath("$.data.items.length()").value(2))
				.andExpect(
					jsonPath("$.data.items[?(@.auctionId == " + liveAuction.getId() + ")]").exists())
				.andExpect(
					jsonPath("$.data.items[?(@.auctionId == " + endingSoonAuction.getId() + ")]")
						.exists())
				// product1(liveAuction)은 북마크 true여야 함
				.andExpect(jsonPath(
					"$.data.items[?(@.auctionId == " + liveAuction.getId() + ")].isBookmarked")
					.value(true))
				// product3(endingSoonAuction)은 북마크 false여야 함
				.andExpect(jsonPath(
					"$.data.items[?(@.auctionId == " + endingSoonAuction.getId() + ")].isBookmarked")
					.value(false));
		}

		@Test
		@WithMockUser(username = "buyer@test.com")
		@DisplayName("카테고리 필터링 - STARGOODS")
		void t2() throws Exception {
			ResultActions resultActions = mockMvc.perform(
				get("/api/v1/auctions")
					.param("category", "STARGOODS")
					.param("status", "LIVE")
					.param("size", "10")
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			resultActions
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(2))
				.andExpect(jsonPath("$.data.items[0].category").value("STARGOODS"));
		}

		@Test
		@WithMockUser(username = "buyer@test.com")
		@DisplayName("실패 - 잘못된 정렬 방식")
		void t3() throws Exception {
			ResultActions resultActions = mockMvc.perform(
				get("/api/v1/auctions")
					.param("sortType", "invalid_sort")
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			resultActions.andExpect(status().isBadRequest());
		}

		@Test
		@WithMockUser(username = "buyer@test.com")
		@DisplayName("커서 기반 페이징 - 다음 페이지 요청 흐름")
		void t4() throws Exception {
			for (int i = 0; i < 5; i++) {
				Product product = createProduct(seller, "추가 상품 " + i, Product.Category.STARGOODS);
				createAuction(product, AuctionStatus.LIVE, LocalDateTime.now().plusHours(24 + i));
			}

			// 1. 첫 페이지
			ResultActions firstPage = mockMvc.perform(
				get("/api/v1/auctions")
					.param("size", "3")
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			firstPage
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(3))
				.andExpect(jsonPath("$.data.hasNext").value(true));

			String responseBody = firstPage.andReturn().getResponse().getContentAsString();
			JsonNode jsonNode = objectMapper.readTree(responseBody);
			String nextCursor = jsonNode.path("data").path("cursor").asText();

			// 2. 다음 페이지
			ResultActions secondPage = mockMvc.perform(
				get("/api/v1/auctions")
					.param("cursor", nextCursor)
					.param("size", "3")
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			secondPage
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items").isArray());
		}
	}

	@Nested
	@DisplayName("경매 상세 조회")
	class GetAuctionDetail {

		@Test
		@WithMockUser(username = "buyer@test.com")
		@DisplayName("성공 - 기본 상세 정보 및 북마크 Redis 캐싱 적용")
		void t1() throws Exception {
			given(bookmarkCacheService.getBookmarkedProductIds(buyer.getId()))
				.willReturn(Set.of(product1.getId()));

			ResultActions resultActions = mockMvc.perform(
				get("/api/v1/auctions/{auctionId}", liveAuction.getId())
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			resultActions
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.auctionId").value(liveAuction.getId()))
				.andExpect(jsonPath("$.data.name").value("한정판 아이돌 굿즈"))
				.andExpect(jsonPath("$.data.isBookmarked").value(true))
				.andExpect(jsonPath("$.data.currentHighestBid").isNumber());
		}

		@Test
		@WithMockUser(username = "buyer@test.com")
		@DisplayName("실패 - 존재하지 않는 경매")
		void t2() throws Exception {
			mockMvc.perform(get("/api/v1/auctions/{auctionId}", 9999L))
				.andDo(print())
				.andExpect(status().isNotFound());
		}

		@Test
		@DisplayName("성공 - 로그인하지 않은 사용자의 경매 상세 조회")
		void t3() throws Exception {
			mockMvc.perform(get("/api/v1/auctions/{auctionId}", liveAuction.getId()))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.isBookmarked").value(false));
		}
	}

	@Nested
	@DisplayName("입찰 및 최고가 조회")
	class BidAndHistory {
		@Test
		@DisplayName("현재 최고가 조회 - 입찰 내역 및 마스킹 확인")
		void t1() throws Exception {
			mockMvc.perform(get("/api/v1/auctions/{auctionId}/highest-bid", endedAuction.getId()))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.currentHighestBid").value(1300000))
				.andExpect(jsonPath("$.data.bidderNickname").value("구매***"));
		}

		@Test
		@DisplayName("입찰 내역 조회 - 페이지네이션")
		void t3() throws Exception {
			mockMvc.perform(
					get("/api/v1/auctions/{auctionId}/bids", endedAuction.getId())
						.param("page", "0")
						.param("size", "5")
				).andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content.length()").value(3));
		}

		@Nested
		@DisplayName("홈 화면 조회 API")
		class Home {
			@Test
			@WithMockUser(username = "buyer@test.com")
			@DisplayName("성공: 로그인한 사용자의 홈 화면 조회 (Redis 캐싱 적용)")
			void getHomeData_withLogin_success() throws Exception {
				// given: Redis Mocking
				given(bookmarkCacheService.getBookmarkedProductIds(buyer.getId()))
					.willReturn(Set.of(product1.getId(), product3.getId()));

				ResultActions resultActions = mockMvc.perform(get("/api/v1/auctions/home"))
					.andDo(print());

				resultActions
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.endingSoon").isNotEmpty())
					.andExpect(jsonPath("$.data.popular").isNotEmpty());

				// then: Mock Service 호출 확인
				then(bookmarkCacheService).should(atLeastOnce()).getBookmarkedProductIds(buyer.getId());
			}

			@Test
			@DisplayName("성공: 로그인하지 않은 사용자의 홈 화면 조회")
			void getHomeData_anonymous_success() throws Exception {
				given(bookmarkCacheService.getBookmarkedProductIds(isNull()))
					.willReturn(null);

				mockMvc.perform(get("/api/v1/auctions/home"))
					.andDo(print())
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.endingSoon[0].isBookmarked").value(false));
			}
		}
	}

	@Nested
	@DisplayName("북마크 Redis 캐싱 동기화 테스트")
	class BookmarkRedisSyncTest {

		@Test
		@WithMockUser(username = "buyer@test.com")
		@DisplayName("북마크 추가 시 Redis 캐시 추가 메서드 호출 확인")
		void addBookmark_shouldCallRedisCache() throws Exception {
			// given
			Product bookmarkProduct = createProduct(seller, "북마크 테스트 상품", Product.Category.STARGOODS);

			// when
			mockMvc.perform(
					post("/api/v1/products/{productId}/bookmarks", bookmarkProduct.getId())
						.contentType(MediaType.APPLICATION_JSON)
				).andDo(print())
				.andExpect(status().isOk());

			// then: Service Mock이 호출되었는지 검증
			then(bookmarkCacheService).should().addBookmark(buyer.getId(), bookmarkProduct.getId());
		}

		@Test
		@WithMockUser(username = "buyer@test.com")
		@DisplayName("북마크 삭제 시 Redis 캐시 삭제 메서드 호출 확인")
		void removeBookmark_shouldCallRedisCache() throws Exception {
			// given
			Product bookmarkProduct = createProduct(seller, "북마크 삭제 테스트 상품", Product.Category.STARGOODS);
			mockMvc.perform(post("/api/v1/products/{productId}/bookmarks", bookmarkProduct.getId()));

			// when
			mockMvc.perform(
					delete("/api/v1/products/{productId}/bookmarks", bookmarkProduct.getId())
						.contentType(MediaType.APPLICATION_JSON)
				).andDo(print())
				.andExpect(status().isOk());

			// then: Service Mock 호출 검증
			then(bookmarkCacheService).should().removeBookmark(buyer.getId(), bookmarkProduct.getId());
		}

		@Test
		@WithMockUser(username = "buyer@test.com")
		@DisplayName("북마크 추가 시 Redis 작업이 실패(예외 발생)해도 DB 작업은 성공하고, Fallback으로 캐시 무효화를 호출해야 한다")
		void addBookmark_redisFailure_shouldNotAffectDB() throws Exception {
			// given
			Product bookmarkProduct = createProduct(seller, "Redis 실패 테스트 상품", Product.Category.STARGOODS);

			// [핵심] Redis 추가 시 Exception이 발생한다고 Mocking
			willThrow(new RuntimeException("Redis 연결 실패"))
				.given(bookmarkCacheService).addBookmark(anyLong(), anyLong());

			// when
			mockMvc.perform(
					post("/api/v1/products/{productId}/bookmarks", bookmarkProduct.getId())
						.contentType(MediaType.APPLICATION_JSON)
				).andDo(print())
				// then: 500 에러가 아니라 200 OK여야 함 (DB는 성공했으므로)
				.andExpect(status().isOk());

			// then: Fallback 로직(캐시 삭제)이 호출되었는지 확인
			then(bookmarkCacheService).should().invalidateUserBookmarkCache(buyer.getId());
		}
	}
}
