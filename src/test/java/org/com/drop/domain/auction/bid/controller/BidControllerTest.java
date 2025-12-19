package org.com.drop.domain.auction.bid.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.com.drop.domain.auction.bid.dto.request.BidRequestDto;
import org.com.drop.domain.auction.bid.dto.response.BidResponseDto;
import org.com.drop.domain.auction.bid.service.BidService;
import org.com.drop.domain.auth.jwt.JwtProvider;
import org.com.drop.domain.auth.service.RefreshTokenFilter;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.service.UserService;
import org.com.drop.global.exception.ErrorCode;
import org.com.drop.global.exception.GlobalExceptionHandler;
import org.com.drop.global.exception.ServiceException;
import org.com.drop.global.security.auth.LoginUserArgumentResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@WebMvcTest(BidController.class)
@AutoConfigureMockMvc
@Import(GlobalExceptionHandler.class)
class BidControllerTest {


	MockMvc mockMvc;

	@MockitoBean
	JwtProvider jwtProvider;

	@MockitoBean
	private RefreshTokenFilter refreshTokenFilter;

	@MockitoBean
	UserService userService;

	@MockitoBean
	BidService bidService;

	@DisplayName("입찰요청이 오면 200과 BidResponseDto를 반환한다")
	@Test
	void returns200AndBidResponseDtoWhenBidRequested() throws Exception {
		// given
		Long auctionId = 12345L;
		Long userId = 1L;
		String email = "test@test.com";
		LocalDateTime bidTime = LocalDateTime.of(2025, 12, 5, 15, 50);

		User appUser = User.builder().id(userId).email(email).build();

		given(userService.findUserByEmail(eq(email))).willReturn(appUser);

		BidResponseDto serviceResponse = BidResponseDto.of(auctionId, true, 11_000L, bidTime);
		given(bidService.placeBid(any(), any(), any())).willReturn(serviceResponse);

		UserDetails securityUser = org.springframework.security.core.userdetails.User
			.withUsername(email)
			.password("password")
			.roles("USER")
			.build();

		Authentication auth = new UsernamePasswordAuthenticationToken(securityUser, null,
			securityUser.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(auth);

		LoginUserArgumentResolver realResolver = new LoginUserArgumentResolver(userService);

		mockMvc = MockMvcBuilders.standaloneSetup(new BidController(bidService))
			.setCustomArgumentResolvers(realResolver)
			.build();

		// when & then
		mockMvc.perform(
				post("/auctions/{auctionId}/bids", auctionId)
					.with(csrf())
					.contentType(String.valueOf(MediaType.APPLICATION_JSON))
					.content("""
						{ "bidAmount": 11000 }
						""")
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.auctionId").value(12345))
			.andExpect(jsonPath("$.data.currentHighestBid").value(11000))
			.andExpect(jsonPath("$.data.isHighestBidder").value(true))
			.andExpect(jsonPath("$.data.bidTime").value("2025-12-05T15:50:00"));
	}

	@DisplayName("최소입찰단위 미만이면 400으로 에러응답한다")
	@Test
	@WithMockUser(username = "test@test.com")
	void returns400WhenBidAmountBelowBidUnit() throws Exception {
		// given
		Long auctionId = 12345L;
		String email = "test@test.com";

		User appUser = User.builder().id(1L).email(email).build();
		given(userService.findUserByEmail(email)).willReturn(appUser);

		UserDetails securityUser = org.springframework.security.core.userdetails.User
			.withUsername(email)
			.password("password")
			.roles("USER")
			.build();

		Authentication auth = new UsernamePasswordAuthenticationToken(securityUser,
			null, securityUser.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(auth);

		LoginUserArgumentResolver realResolver = new LoginUserArgumentResolver(userService);

		mockMvc = MockMvcBuilders.standaloneSetup(new BidController(bidService))
			.setCustomArgumentResolvers(realResolver)
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();

		given(bidService.placeBid(auctionId, appUser.getEmail(), new BidRequestDto(10000L)))
			.willThrow(new ServiceException(ErrorCode.AUCTION_BID_AMOUNT_TOO_LOW,
				"입찰 금액이 현재 최고가보다 낮거나 최소 입찰 단위를 충족하지 못했습니다."));

		// when & then
		mockMvc.perform(
				post("/auctions/{auctionId}/bids", auctionId)
					.with(csrf())
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

	@DisplayName("로그인하지 않은 사용자가 입찰요청시 403 응답한다")
	@Test
	void returns401WhenAnonymousUserBids() throws Exception {
		// given
		Long auctionId = 12345L;

		// when & then
		mockMvc.perform(
				post("/auctions/{auctionId}/bids", auctionId)
					.contentType(String.valueOf(MediaType.APPLICATION_JSON))
					.content("""
							{ "bidAmount": 10000 }
						""")
			)
			.andDo(print())
			.andExpect(status().isForbidden());
	}
}
