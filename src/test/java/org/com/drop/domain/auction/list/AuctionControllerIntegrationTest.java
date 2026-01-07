package org.com.drop.domain.auction.list;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.com.drop.BaseIntegrationTest;
import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.auction.entity.Auction.AuctionStatus;
import org.com.drop.domain.auction.auction.repository.AuctionRepository;
import org.com.drop.domain.auction.bid.entity.Bid;
import org.com.drop.domain.auction.bid.repository.BidRepository;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Transactional
@DisplayName("경매 컨트롤러 통합 테스트")
class AuctionControllerIntegrationTest extends BaseIntegrationTest {

	@Autowired MockMvc mockMvc;
	@Autowired ObjectMapper objectMapper;
	@Autowired UserRepository userRepository;
	@Autowired ProductRepository productRepository;
	@Autowired ProductImageRepository productImageRepository;
	@Autowired AuctionRepository auctionRepository;
	@Autowired BidRepository bidRepository;

	/**
	 * AmazonS3Client를 Mock으로 대체.
	 */
	@MockitoBean
	private AmazonS3Client amazonS3Client;

	private User seller;
	private User buyer;
	private Auction liveAuction;
	private Auction endedAuction;

	@BeforeEach
	void setUp() {
		// 1. 유저 생성
		seller = createUser("seller@test.com", "판매자");
		buyer = createUser("buyer@test.com", "구매자");

		// 2. 상품 생성
		Product product1 = createProduct(seller, "한정판 아이돌 굿즈", Product.Category.STARGOODS);
		Product product2 = createProduct(seller, "희귀 피규어", Product.Category.FIGURE);

		// 3. 경매 생성
		liveAuction = createAuction(product1, AuctionStatus.LIVE, LocalDateTime.now().plusHours(24));
		endedAuction = createAuction(product2, AuctionStatus.ENDED, LocalDateTime.now().minusDays(1));

		// 4. 입찰 생성 (종료된 경매에 입찰 내역 추가)
		createBid(endedAuction, buyer, 1200000L);
		createBid(endedAuction, seller, 1100000L);
		createBid(endedAuction, buyer, 1300000L);
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
			.description("설명")
			.category(category)
			.subcategory(Product.SubCategory.ETC)
			.createdAt(LocalDateTime.now())
			.bookmarkCount(0)
			.build());
		productImageRepository.save(new ProductImage(product, "test.jpg"));
		return product;
	}

	private Auction createAuction(Product product, AuctionStatus status, LocalDateTime endAt) {
		return auctionRepository.save(Auction.builder()
			.product(product)
			.startPrice(1000000)
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
	}

	@Nested
	@DisplayName("경매 목록 조회")
	class GetAuctions {

		@Test
		@WithMockUser(username = "buyer@test.com")
		@DisplayName("기본 조회 - LIVE 상태 확인")
		void t1() throws Exception {
			ResultActions resultActions = mockMvc.perform(
				get("/api/v1/auctions")
					.param("status", "LIVE")
					.param("size", "10")
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			resultActions
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items").isArray())
				.andExpect(jsonPath("$.data.items[0].auctionId").value(liveAuction.getId()))
				.andExpect(jsonPath("$.data.items[0].status").value("LIVE"));
		}

		@Test
		@WithMockUser(username = "buyer@test.com")
		@DisplayName("카테고리 필터링 - STARGOODS")
		void t2() throws Exception {
			ResultActions resultActions = mockMvc.perform(
				get("/api/v1/auctions")
					.param("category", "STARGOODS")
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			resultActions
				.andExpect(status().isOk())
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

			resultActions
				.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("커서 기반 페이징 - 다음 페이지 요청 흐름")
		void t4() throws Exception {
			// 1. 첫 페이지 요청 (size=1)
			ResultActions firstPage = mockMvc.perform(
				get("/api/v1/auctions")
					.param("size", "1")
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			firstPage
				.andExpect(status().isOk())
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
					.param("size", "1")
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
		@DisplayName("성공 - 기본 상세 정보")
		void t1() throws Exception {
			ResultActions resultActions = mockMvc.perform(
				get("/api/v1/auctions/{auctionId}", liveAuction.getId())
					.contentType(MediaType.APPLICATION_JSON)
			).andDo(print());

			resultActions
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.auctionId").value(liveAuction.getId()))
				.andExpect(jsonPath("$.data.name").value("한정판 아이돌 굿즈"))
				.andExpect(jsonPath("$.data.isBookmarked").exists());
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
				// "구매자" -> "구매***"
				.andExpect(jsonPath("$.data.bidderNickname").value("구매***"));
		}

		@Test
		@DisplayName("입찰 내역 조회 - 페이지네이션")
		void t2() throws Exception {
			ResultActions resultActions = mockMvc.perform(
				get("/api/v1/auctions/{auctionId}/bids", endedAuction.getId())
					.param("page", "0")
					.param("size", "5")
			).andDo(print());

			resultActions
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content.length()").value(3))
				.andExpect(jsonPath("$.data.totalElements").value(3));
		}

		@Nested
		@DisplayName("홈 화면 조회 API")
		class Home {
			@Test
			@DisplayName("성공: 홈 화면 데이터(마감 임박, 인기 경매)를 조회해야 한다")
			void getHomeData_success() throws Exception {
				mockMvc.perform(get("/api/v1/auctions/home"))
					.andDo(print())
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.endingSoon").isArray())
					.andExpect(jsonPath("$.data.popular").isArray());
			}
		}
	}
}
