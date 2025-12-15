package org.com.drop.domain.auction.bid.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.com.drop.domain.auction.bid.dto.response.BuyNowResponseDto;
import org.com.drop.domain.auction.bid.service.BuyNowService;
import org.com.drop.domain.auth.jwt.JwtProvider;
import org.com.drop.global.exception.ErrorCode;
import org.com.drop.global.exception.ServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BuyNowController.class)
@AutoConfigureMockMvc(addFilters = false)
class BuyNowControllerTest {
	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	JwtProvider jwtProvider;


	@MockitoBean
	BuyNowService buyNowService;



	@Test
	void 즉시구매_요청시_200과_ResponseCustom_OK_형태로_데이터를_반환한다() throws Exception {
		// given
		Long auctionId = 12345L;
		Long userId = 987L;

		BuyNowResponseDto serviceResponse = new BuyNowResponseDto(
			auctionId,
			"ENDED",
			555L,
			50_000,
			LocalDateTime.of(2025, 12, 5, 15, 50, 0)
		);

		given(buyNowService.buyNow(eq(auctionId), eq(userId)))
			.willReturn(serviceResponse);

		// when & then
		mockMvc.perform(
				post("/auctions/{auctionId}/buy-now", auctionId)
					.queryParam("userId", String.valueOf(userId))
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.auctionId").value(12345))
			.andExpect(jsonPath("$.data.auctionStatus").value("ENDED"))
			.andExpect(jsonPath("$.data.winnerId").value(555))
			.andExpect(jsonPath("$.data.finalPrice").value(50000))
			.andExpect(jsonPath("$.data.winTime").value("2025-12-05T15:50:00"));
	}

	@Test
	void 즉시구매가_설정되지_않은_경매면_400과_ErrorResponse를_반환한다() throws Exception {
		// given
		Long auctionId = 12345L;
		Long userId = 987L;

		given(buyNowService.buyNow(eq(auctionId), eq(userId)))
			.willThrow(new ServiceException(ErrorCode.AUCTION_BUY_NOW_NOT_AVAILABLE));

		// when & then
		mockMvc.perform(
				post("/auctions/{auctionId}/buy-now", auctionId)
					.queryParam("userId", String.valueOf(userId))
			)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("AUCTION_BUY_NOW_NOT_AVAILABLE"))
			.andExpect(jsonPath("$.status").value("400"))
			.andExpect(jsonPath("$.message").exists());
	}
}
