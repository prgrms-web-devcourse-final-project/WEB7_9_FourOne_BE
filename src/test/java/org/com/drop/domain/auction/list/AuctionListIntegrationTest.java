package org.com.drop.domain.auction.list;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.com.drop.domain.auction.list.dto.SortType;
import org.com.drop.domain.auction.product.entity.Product.Category;
import org.com.drop.global.aws.AmazonS3Client;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("경매 목록 조회 통합 테스트")
@Import({TestDataSetup.class, TestLoginUserArgumentResolverConfig.class})
class AuctionListIntegrationTest {

	@MockitoBean
	AmazonS3Client amazonS3Client;

	@Autowired MockMvc mockMvc;
	@Autowired TestDataSetup.AuctionTestData testData; // ✅ setup 데이터 주입

	@Test
	@DisplayName("비로그인 사용자는 경매 목록 조회 가능")
	void getAuctions_anonymous() throws Exception {
		mockMvc.perform(get("/api/v1/auction-list")
				.param("sort", SortType.NEWEST.name())
				.param("size", "10")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items").isArray())
			.andExpect(jsonPath("$.data.items.length()", greaterThan(0)))
			.andExpect(jsonPath("$.data.items[0].isBookmarked").value(false));
	}

	@Test
	@DisplayName("로그인 사용자는 북마크 여부가 반영된다")
	void getAuctions_loggedIn() throws Exception {
		String email = testData.viewer().getEmail();

		mockMvc.perform(get("/api/v1/auction-list")
				.param("sort", SortType.NEWEST.name())
				.param("size", "10")
				.with(user(email).roles("USER")))
			.andExpect(status().isOk())
			// viewer가 북마크한 상품이 있으니 true가 최소 1개는 나와야 함
			.andExpect(jsonPath("$.data.items[*].isBookmarked", hasItem(true)));
	}

	@Test
	@DisplayName("카테고리 필터링 - STARGOODS")
	void filter_by_category() throws Exception {
		mockMvc.perform(get("/api/v1/auction-list")
				.param("category", Category.STARGOODS.name())
				.param("sort", SortType.NEWEST.name())
				.param("size", "10"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items[*].category",
				everyItem(is(Category.STARGOODS.name()))));
	}

	@Test
	@DisplayName("경매 상세 조회 - 입찰/이미지 있는 경매")
	void auction_detail() throws Exception {
		Long auctionId = testData.liveEndingSoonAuction().getId(); // ✅ 1L 고정 금지

		mockMvc.perform(get("/api/v1/auction-list/{auctionId}", auctionId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.auctionId").value(auctionId))
			.andExpect(jsonPath("$.data.imageUrls").isArray())
			.andExpect(jsonPath("$.data.imageUrls.length()", greaterThan(0)))
			.andExpect(jsonPath("$.data.totalBidCount").value(greaterThan(0)));
	}

	@Test
	@DisplayName("현재 최고 입찰가 조회 - 입찰 있음이면 bidderNickname은 마스킹된 값이 내려온다")
	void get_highest_bid() throws Exception {
		Long auctionId = testData.liveEndingSoonAuction().getId();

		mockMvc.perform(get("/api/v1/auction-list/{auctionId}/highest-bid", auctionId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.currentHighestBid").value(30000))
			// viewer nickname이 "조회자"라면 maskNickname => "조회***"
			.andExpect(jsonPath("$.data.bidderNickname").value("조회***"));
	}
}
