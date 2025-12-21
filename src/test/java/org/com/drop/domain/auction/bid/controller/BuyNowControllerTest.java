package org.com.drop.domain.auction.bid.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.com.drop.domain.auction.bid.dto.response.BuyNowResponseDto;
import org.com.drop.domain.auction.bid.service.BuyNowService;
import org.com.drop.domain.auth.jwt.JwtProvider;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.service.UserService;
import org.com.drop.global.exception.ErrorCode;
import org.com.drop.global.exception.GlobalExceptionHandler;
import org.com.drop.global.exception.ServiceException;
import org.com.drop.global.security.auth.LoginUserArgumentResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@WebMvcTest(BuyNowController.class)
@AutoConfigureMockMvc(addFilters = false)
class BuyNowControllerTest {
	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	JwtProvider jwtProvider;

	@MockitoBean
	UserService userService;

	@MockitoBean
	BuyNowService buyNowService;

	@DisplayName("즉시구매_요청시_200과_ResponseCustom_OK_형태로_데이터를_반환한다")
	@Test
	void returns200AndResponseCustomOkWhenBuyNowRequested() throws Exception {
		// given
		Long auctionId = 12345L;
		Long userId = 987L;
		String email = "buyer@test.com";

		User appUser = User.builder()
			.id(userId)
			.email(email)
			.build();
		given(userService.findUserByEmail(email)).willReturn(appUser);

		UserDetails securityUser = org.springframework.security.core.userdetails.User
			.withUsername(email)
			.password("password")
			.roles("USER")
			.build();
		Authentication auth = new UsernamePasswordAuthenticationToken(securityUser, null,
			securityUser.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(auth);

		BuyNowResponseDto serviceResponse = new BuyNowResponseDto(
			auctionId,
			"ENDED",
			555L,
			50_000L,
			LocalDateTime.of(2025, 12, 5, 15, 50, 0)
		);

		given(buyNowService.buyNow(eq(auctionId), eq(userId)))
			.willReturn(serviceResponse);

		mockMvc = MockMvcBuilders.standaloneSetup(new BuyNowController(buyNowService))
			.setCustomArgumentResolvers(new LoginUserArgumentResolver(userService))
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();

		// when & then
		mockMvc.perform(
				post("/auctions/{auctionId}/buy-now", auctionId)
					.contentType("application/json")
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.auctionId").value(12345))
			.andExpect(jsonPath("$.data.auctionStatus").value("ENDED"))
			.andExpect(jsonPath("$.data.winnerId").value(555))
			.andExpect(jsonPath("$.data.finalPrice").value(50000))
			.andExpect(jsonPath("$.data.winTime").value("2025-12-05T15:50:00"));
	}

	@DisplayName("즉시구매가 설정되지 않은 경매면 400과 ErrorResponse를 반환한다")
	@Test
	void returns400AndErrorResponseWhenBuyNowPriceIsNotSet() throws Exception {
		// given
		Long auctionId = 12345L;
		Long userId = 1L;
		String email = "test@test.com";

		User appUser = User.builder().id(userId).email(email).build();

		given(userService.findUserByEmail(eq(email))).willReturn(appUser);

		UserDetails securityUser = org.springframework.security.core.userdetails.User
			.withUsername(email)
			.password("password")
			.roles("USER")
			.build();

		Authentication auth = new UsernamePasswordAuthenticationToken(securityUser, null,
			securityUser.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(auth);

		given(buyNowService.buyNow(eq(auctionId), eq(userId)))
			.willThrow(new ServiceException(ErrorCode.AUCTION_BUY_NOW_NOT_AVAILABLE, "즉시 구매가 불가능한 상품입니다."));

		// when & then
		mockMvc.perform(
				post("/auctions/{auctionId}/buy-now", auctionId)
					.contentType("application/json")
			)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.httpStatus").value(400))
			.andExpect(jsonPath("$.code").value("AUCTION_BUY_NOW_NOT_AVAILABLE"))
			.andExpect(jsonPath("$.message").exists());
	}
}
