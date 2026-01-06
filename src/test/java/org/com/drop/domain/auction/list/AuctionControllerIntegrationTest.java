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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
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
		cacheManager.getCacheNames().forEach(name -> {
			Cache cache = cacheManager.getCache(name);
			if (cache != null) {
				cache.clear();
			}
		});

		// 1. 유저 생성
		seller = createUser("seller@test.com", "판매자");
		buyer = createUser("buyer@test.com", "구매자");

		// 2. 상품 생성
		product1 = createProduct(seller, "한정판 아이돌 굿즈", Product.Category.STARGOODS);
		product2 = createProduct(seller, "희귀 피규어", Product.Category.FIGURE);
		product3 = createProduct(seller, "마감임박 상품", Product.Category.STARGOODS);

		// 3. 경매 생성 - 생성 순서를 liveAuction이 먼저 오도록 변경
		// liveAuction을 먼저 생성하고, endingSoonAuction을 나중에 생성
		liveAuction = createAuction(
			product1, AuctionStatus.LIVE, LocalDateTime.now().plusHours(24));
		endingSoonAuction = createAuction(
			product3, AuctionStatus.LIVE, LocalDateTime.now().plusHours(3)); // 3시간 후 마감
		endedAuction = createAuction(
			product2, AuctionStatus.ENDED, LocalDateTime.now().minusDays(1));

		// 4. 입찰 생성 (종료된 경매에 입찰 내역 추가)
		createBid(endedAuction, buyer, 1200000L);
		createBid(endedAuction, seller, 1100000L);
		createBid(endedAuction, buyer, 1300000L);

		// 인기 경매를 위해 입찰 추가
		createBid(liveAuction, buyer, 1100000L);
		createBid(liveAuction, seller, 1200000L);

		// 5. AmazonS3Client Mock 설정
		given(amazonS3Client.getPresignedUrl(anyString()))
			.willAnswer(
				invocation -> "https://s3.amazonaws.com/test-bucket/" + invocation.getArgument(0));

		// 6. BookmarkCacheService Mock 설정 - 기본값
		// 로그인하지 않은 사용자는 null 반환
		given(bookmarkCacheService.getBookmarkedProductIds(isNull()))
			.willReturn(null);

		given(bookmarkCacheService.getBookmarkedProductIds(anyLong()))
			.willReturn(Set.of());

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

		@Test
		@WithMockUser(username = "buyer@test.com")
		@DisplayName("기본 조회 - LIVE 상태 확인 및 북마크 Redis 캐싱 적용")
		void t1() throws Exception {
			// given: buyer의 북마크 목록 설정
			given(bookmarkCacheService.getBookmarkedProductIds(buyer.getId()))
				.willReturn(Set.of(product1.getId())); // product1은 북마크됨

			ResultActions resultActions = mockMvc.perform(
				get("/api/v1/auctions")
					.param("status", "LIVE")
					.param("size", "10")
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			resultActions
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items").isArray())
				.andExpect(jsonPath("$.data.items.length()").value(2)) // 2개의 LIVE 경매
				// liveAuction이 응답에 포함되어 있는지 확인
				.andExpect(
					jsonPath("$.data.items[?(@.auctionId == " + liveAuction.getId() + ")]")
						.exists())
				// endingSoonAuction이 응답에 포함되어 있는지 확인
				.andExpect(
					jsonPath("$.data.items[?(@.auctionId == " + endingSoonAuction.getId() + ")]")
						.exists())
				// liveAuction의 북마크 여부가 true인지 확인
				.andExpect(
					jsonPath(
						"$.data.items[?(@.auctionId == " + liveAuction.getId() + ")].isBookmarked")
						.value(true))
				// endingSoonAuction의 북마크 여부가 false인지 확인
				.andExpect(
					jsonPath(
						"$.data.items[?(@.auctionId == " + endingSoonAuction.getId()
							+ ")].isBookmarked")
						.value(false));
		}

		@Test
		@WithMockUser(username = "buyer@test.com")
		@DisplayName("카테고리 필터링 - STARGOODS")
		void t2() throws Exception {
			// given: STARGOODS 카테고리 필터링
			ResultActions resultActions = mockMvc.perform(
				get("/api/v1/auctions")
					.param("category", "STARGOODS")
					.param("status", "LIVE")
					.param("size", "10")
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			resultActions
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items").isArray())
				.andExpect(jsonPath("$.data.items.length()").value(2)) // 2개의 STARGOODS 경매
				.andExpect(jsonPath("$.data.items[0].category").value("STARGOODS"))
				.andExpect(jsonPath("$.data.items[1].category").value("STARGOODS"));
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

			resultActions
				.andExpect(status().isBadRequest());
		}

		@Test
		@WithMockUser(username = "buyer@test.com")
		@DisplayName("커서 기반 페이징 - 다음 페이지 요청 흐름")
		void t4() throws Exception {
			// given: 추가 경매 생성으로 페이징 테스트
			for (int i = 0; i < 5; i++) {
				Product product = createProduct(seller, "추가 상품 " + i, Product.Category.STARGOODS);
				createAuction(product, AuctionStatus.LIVE, LocalDateTime.now().plusHours(24 + i));
			}

			// 1. 첫 페이지 요청 (size=3)
			ResultActions firstPage = mockMvc.perform(
				get("/api/v1/auctions")
					.param("size", "3")
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			firstPage
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(3))
				.andExpect(jsonPath("$.data.hasNext").value(true))
				.andExpect(jsonPath("$.data.cursor").isNotEmpty());

			// 2. 커서 추출
			String responseBody = firstPage.andReturn().getResponse().getContentAsString();
			JsonNode jsonNode = objectMapper.readTree(responseBody);
			String nextCursor = jsonNode.path("data").path("cursor").asText();

			// 3. 다음 페이지 요청 (추출한 커서 사용)
			ResultActions secondPage = mockMvc.perform(
				get("/api/v1/auctions")
					.param("cursor", nextCursor)
					.param("size", "3")
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			secondPage
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items").isArray())
				.andExpect(jsonPath("$.data.items.length()").value(3));
		}
	}

	@Nested
	@DisplayName("경매 상세 조회")
	class GetAuctionDetail {

		@Test
		@WithMockUser(username = "buyer@test.com")
		@DisplayName("성공 - 기본 상세 정보 및 북마크 Redis 캐싱 적용")
		void t1() throws Exception {
			// given: buyer가 product1을 북마크
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
				.andExpect(jsonPath("$.data.isBookmarked").value(true)) // Redis에서 북마크 여부 조회
				.andExpect(jsonPath("$.data.imageUrls").isArray()) // 이미지 URL 배열
				.andExpect(jsonPath("$.data.currentHighestBid").isNumber()) // 현재 최고가
				.andExpect(jsonPath("$.data.remainingTimeSeconds").isNumber()); // 남은 시간
		}

		@Test
		@WithMockUser(username = "buyer@test.com")
		@DisplayName("실패 - 존재하지 않는 경매")
		void t2() throws Exception {
			ResultActions resultActions = mockMvc.perform(
				get("/api/v1/auctions/{auctionId}", 9999L)
			).andDo(print());

			resultActions
				.andExpect(status().isNotFound());
		}

		@Test
		@DisplayName("성공 - 로그인하지 않은 사용자의 경매 상세 조회")
		void t3() throws Exception {
			// when: 로그인 없이 조회
			ResultActions resultActions = mockMvc.perform(
				get("/api/v1/auctions/{auctionId}", liveAuction.getId())
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			resultActions
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.isBookmarked").value(false)) // 로그인 안했으므로 false
				.andExpect(jsonPath("$.data.sellerNickname").value("판매자"));
		}
	}

	@Nested
	@DisplayName("입찰 및 최고가 조회")
	class BidAndHistory {
		@Test
		@DisplayName("현재 최고가 조회 - 입찰 내역 및 마스킹 확인")
		void t1() throws Exception {
			ResultActions resultActions = mockMvc.perform(
				get("/api/v1/auctions/{auctionId}/highest-bid", endedAuction.getId())
			).andDo(print());

			resultActions
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.currentHighestBid").value(1300000))
				.andExpect(jsonPath("$.data.bidderNickname").value("구매***")); // 닉네임 마스킹 적용
		}

		@Test
		@DisplayName("현재 최고가 조회 - 입찰 내역 없는 경매")
		void t2() throws Exception {
			// given: 입찰이 없는 경매 생성
			Product product4 = createProduct(seller, "입찰 없는 상품", Product.Category.GAME);
			Auction noBidAuction = createAuction(
				product4, AuctionStatus.LIVE, LocalDateTime.now().plusHours(24));

			ResultActions resultActions = mockMvc.perform(
				get("/api/v1/auctions/{auctionId}/highest-bid", noBidAuction.getId())
			).andDo(print());

			resultActions
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.currentHighestBid").value(1000000)) // 시작가 반환
				.andExpect(jsonPath("$.data.bidderNickname").isEmpty()); // 입찰자 없음
		}

		@Test
		@DisplayName("입찰 내역 조회 - 페이지네이션")
		void t3() throws Exception {
			ResultActions resultActions = mockMvc.perform(
				get("/api/v1/auctions/{auctionId}/bids", endedAuction.getId())
					.param("page", "0")
					.param("size", "5")
			).andDo(print());

			resultActions
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content.length()").value(3))
				.andExpect(jsonPath("$.data.totalElements").value(3))
				.andExpect(jsonPath("$.data.content[0].bidder").value("구매***")) // 마스킹 확인
				.andExpect(jsonPath("$.data.content[0].bidAmount").value(1300000));
		}

		@Nested
		@DisplayName("홈 화면 조회 API")
		class Home {

			@Test
			@DisplayName("성공: 홈 화면 데이터(마감 임박, 인기 경매)를 조회해야 한다")
			void getHomeData_success() throws Exception {
				// 캐시 초기화
				Cache homeCache = cacheManager.getCache("homeAuctions");
				if (homeCache != null) {
					homeCache.clear();
				}

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
				Cache homeCache = cacheManager.getCache("homeAuctions");
				if (homeCache != null) {
					homeCache.clear();
				}

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

				if (!endingSoonItems.isEmpty()) {
					// product3 (마감임박 상품)은 북마크됨
					assert endingSoonItems.get(0).path("isBookmarked").asBoolean();
				}

				if (!popularItems.isEmpty()) {
					// product1 (한정판 아이돌 굿즈)은 북마크됨
					assert popularItems.get(0).path("isBookmarked").asBoolean();
				}
			}

			@Test
			@WithMockUser(username = "buyer@test.com")
			@DisplayName("성공: 홈 화면 캐싱 동작 확인")
			void getHomeData_caching_behavior() throws Exception {
				// 캐시 초기화
				Cache homeCache = cacheManager.getCache("homeAuctions");
				if (homeCache != null) {
					homeCache.clear();
				}

				// 첫 번째 요청 - 캐시 미스 (DB 조회)
				ResultActions firstRequest = mockMvc.perform(get("/api/v1/auctions/home"))
					.andDo(print());

				firstRequest.andExpect(status().isOk());

				// 두 번째 요청 - 캐시 히트 (캐시에서 조회)
				ResultActions secondRequest = mockMvc.perform(get("/api/v1/auctions/home"))
					.andDo(print());

				secondRequest.andExpect(status().isOk());

				// BookmarkCacheService가 호출되었는지 확인
				then(bookmarkCacheService).should(atLeastOnce())
					.getBookmarkedProductIds(buyer.getId());
			}

			@Test
			@DisplayName("성공: 로그인하지 않은 사용자의 홈 화면 조회")
			void getHomeData_anonymous_success() throws Exception {
				// 캐시 초기화
				Cache homeCache = cacheManager.getCache("homeAuctions");
				if (homeCache != null) {
					homeCache.clear();
				}

				// given: 비로그인 사용자 북마크 없음
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
					.andExpect(jsonPath("$.data.endingSoon[0].isBookmarked").value(false))
					.andExpect(jsonPath("$.data.popular[0].isBookmarked").value(false));
			}
		}
	}
}
