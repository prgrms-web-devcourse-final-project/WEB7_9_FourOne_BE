package org.com.drop.domain.auction.bid.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.com.drop.domain.auction.bid.dto.request.BidRequestDto;
import org.com.drop.domain.auction.bid.dto.response.BidResponseDto;
import org.com.drop.domain.auction.bid.service.BidService;
import org.com.drop.domain.auth.jwt.JwtProvider;
import org.com.drop.global.exception.ErrorCode;
import org.com.drop.global.exception.ServiceException;
import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BidController.class)
@AutoConfigureMockMvc(addFilters = false)
class BidControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	JwtProvider jwtProvider;

	@MockitoBean
	BidService bidService;

	@Test
	void 입찰요청이_오면_200과_BidResponseDto를_반환한다() throws Exception {
		//given
		Long auctionId = 12345L;
		Long userId = 987L;

		LocalDateTime bidTime = LocalDateTime.of(2025, 12, 5, 15, 50);

		BidResponseDto serviceResponse = BidResponseDto.of(
			auctionId,
			true,
			11_000L,
			bidTime
		);

		given(bidService.placeBid(eq(auctionId), eq(userId), any(BidRequestDto.class)))
			.willReturn(serviceResponse);

		//when&then
		mockMvc.perform(
				post("/bids/{auctionId}/bids", auctionId)
					.queryParam("userId", String.valueOf(userId))
					.contentType(String.valueOf(MediaType.APPLICATION_JSON))
					.content("""
						    { "bidAmount": 11000 }
						""")
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.auctionId").value(12345))
			.andExpect(jsonPath("$.data.isHighestBidder").value(true))
			.andExpect(jsonPath("$.data.currentHighestBid").value(11000))
			.andExpect(jsonPath("$.data.bidTime").value("2025-12-05T15:50:00"));
	}

	@Test
	void 최소입찰단위_미만이면_400으로_에러응답한다() throws Exception {
		// given
		Long auctionId = 12345L;
		Long userId = 987L;
		// 서비스에서 예외 던지도록 설정
		given(bidService.placeBid(auctionId, userId, new BidRequestDto(10000L)))
			.willThrow(new ServiceException(ErrorCode.AUCTION_BID_AMOUNT_TOO_LOW, "입찰 금액이 현재 최고가보다 낮거나 최소 입찰 단위를 충족하지 못했습니다."));

		// when & then
		mockMvc.perform(
				post("/bids/{auctionId}/bids", auctionId)
					.queryParam("userId", String.valueOf(userId))
					.contentType(String.valueOf(MediaType.APPLICATION_JSON))
					.content("""
                        { "bidAmount": 10000 }
                    """)
			)
			.andExpect(status().isBadRequest())
			.andExpect(status().is(400))
			.andExpect(jsonPath("$.code").value("AUCTION_BID_AMOUNT_TOO_LOW")) // 네 ErrorCode 숫자에 맞춰
			.andExpect(jsonPath("$.message").value("입찰 금액이 현재 최고가보다 낮거나 최소 입찰 단위를 충족하지 못했습니다.")); // 메시지는 신경 안 쓴다 했으니 exists만
	}
}
