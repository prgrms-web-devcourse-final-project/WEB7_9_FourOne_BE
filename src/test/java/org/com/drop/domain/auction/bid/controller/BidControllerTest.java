package org.com.drop.domain.auction.bid.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.com.drop.domain.auction.bid.dto.request.BidRequestDto;
import org.com.drop.domain.auction.bid.dto.response.BidHistoryResponse;
import org.com.drop.domain.auction.bid.dto.response.BidResponseDto;
import org.com.drop.domain.auction.bid.service.BidService;
import org.com.drop.domain.auth.jwt.JwtProvider;
import org.com.drop.global.exception.ErrorCode;
import org.com.drop.global.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(BidController.class)
@AutoConfigureMockMvc(addFilters = false)
class BidControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	JwtProvider jwtProvider;

	@MockitoBean
	BidService bidService;

	@DisplayName("입찰요청이 오면 200과 BidResponseDto를 반환한다")
	@Test
	void returns200AndBidResponseDtoWhenBidRequested() throws Exception {
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

	@DisplayName("최소입찰단위 미만이면 400으로 에러응답한다")
	@Test
	void returns400WhenBidAmountBelowBidUnit() throws Exception {
		// given
		Long auctionId = 12345L;
		Long userId = 987L;
		// 서비스에서 예외 던지도록 설정
		given(bidService.placeBid(auctionId, userId, new BidRequestDto(10000L)))
			.willThrow(new ServiceException(ErrorCode.AUCTION_BID_AMOUNT_TOO_LOW,
				"입찰 금액이 현재 최고가보다 낮거나 최소 입찰 단위를 충족하지 못했습니다."));

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
			.andExpect(jsonPath("$.httpStatus").value(400))
			.andExpect(jsonPath("$.code").value("AUCTION_BID_AMOUNT_TOO_LOW"))
			.andExpect(jsonPath("$.message").value("입찰 금액이 현재 최고가보다 낮거나 최소 입찰 단위를 충족하지 못했습니다."))
			.andDo(print());
	}

	@Test
	@DisplayName("입찰 리스트 페이지 요청시 200과 페이지 리스트 형태로 반환한다")
	void canRetrieveBidHistoryAsPagedList() throws Exception {
		// given
		Long auctionId = 1L;

		// 테스트용 응답 데이터 생성 (Builder 사용)
		BidHistoryResponse bid1 = BidHistoryResponse.builder()
			.bidId(100L)
			.bidAmount(50000L)
			.bidder("te***") // 이미 마스킹된 상태라고 가정
			.bidTime(LocalDateTime.now())
			.build();

		BidHistoryResponse bid2 = BidHistoryResponse.builder()
			.bidId(99L)
			.bidAmount(48000L)
			.bidder("us***")
			.bidTime(LocalDateTime.now().minusMinutes(10))
			.build();

		// Service가 리턴할 Page 객체 Mocking
		List<BidHistoryResponse> responseList = List.of(bid1, bid2);
		Page<BidHistoryResponse> pageResult = new PageImpl<>(responseList);

		// Service 메서드 호출 시, 위에서 만든 pageResult를 리턴하도록 설정
		given(bidService.getBidHistory(eq(auctionId), any(Pageable.class)))
			.willReturn(pageResult);

		// when
		ResultActions resultActions = mockMvc.perform(
			get("/api/bids/{auctionId}", auctionId)
				.param("page", "0")
				.param("size", "10")
				.param("sort", "createdAt,desc") // 요청 파라미터 시뮬레이션
				.contentType(String.valueOf(MediaType.APPLICATION_JSON))
				.with(csrf()) // 시큐리티 설정이 있다면 CSRF 토큰 필요
		);

		// then
		resultActions
			.andExpect(status().isOk())
			.andDo(print())
			.andExpect(jsonPath("$.data.content[0].bidId").value(100L))
			.andExpect(jsonPath("$.data.content[0].bidder").value("te***"))
			.andExpect(jsonPath("$.data.content[0].bidAmount").value(50000L))
			.andExpect(jsonPath("$.data.content[1].bidId").value(99L))
			// 페이징 메타데이터 검증
			.andExpect(jsonPath("$.data.size").value(2))      // 현재 페이지의 데이터 수 (PageImpl 기본 동작)
			.andExpect(jsonPath("$.data.totalElements").value(2));
	}


}
