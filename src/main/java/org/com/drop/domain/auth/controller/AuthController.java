package org.com.drop.domain.auth.controller;

import org.com.drop.domain.auth.dto.GetCurrentUserInfoResponse;
import org.com.drop.domain.auth.dto.LocalLoginRequest;
import org.com.drop.domain.auth.dto.LocalLoginResponse;
import org.com.drop.domain.auth.dto.LocalSignUpRequest;
import org.com.drop.domain.auth.dto.LocalSignUpResponse;
import org.com.drop.domain.auth.dto.TokenRefreshResponse;
import org.com.drop.domain.auth.dto.UserDeleteRequest;
import org.com.drop.domain.auth.dto.UserDeleteResponse;
import org.com.drop.domain.auth.email.dto.EmailSendRequest;
import org.com.drop.domain.auth.email.dto.EmailSendResponse;
import org.com.drop.domain.auth.email.dto.EmailVerifyRequest;
import org.com.drop.domain.auth.email.dto.EmailVerifyResponse;
import org.com.drop.domain.auth.service.AuthService;
import org.com.drop.domain.auth.store.RefreshTokenStore;
import org.com.drop.domain.user.entity.User;
import org.com.drop.global.exception.ErrorCode;
import org.com.drop.global.rsdata.RsData;
import org.com.drop.global.security.auth.LoginUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	static final int CACHE_TTL_SECONDS = 7 * 24 * 60 * 60;
	private final AuthService authService;
	private final RefreshTokenStore refreshTokenStore;

	@PostMapping("/local/signup")
	public RsData<LocalSignUpResponse> signup(
		@Valid @RequestBody LocalSignUpRequest dto) {

		LocalSignUpResponse response = authService.signup(dto);

		return new RsData<>(201, response);
	}

	@PostMapping("/email/send-code")
	public RsData<EmailSendResponse> sendVerificationCode(
		@Valid @RequestBody EmailSendRequest dto) {

		EmailSendResponse response = authService.sendVerificationCode(dto.email());

		return new RsData<>(202, response);
	}

	@PostMapping("/email/verify-code")
	public RsData<EmailVerifyResponse> verifyCode(
		@Valid @RequestBody EmailVerifyRequest dto) {

		EmailVerifyResponse response = authService.verifyCode(dto.email(), dto.code());

		return new RsData<>(200, response);
	}

	@PostMapping("/delete")
	public RsData<UserDeleteResponse> deleteAccount(
		@LoginUser User user,
		@Validated @RequestBody UserDeleteRequest request) {

		UserDeleteResponse response = authService.deleteAccount(user, request);

		return new RsData<>(200, response);
	}

	@PostMapping("/login")
	public RsData<LocalLoginResponse> login(
		@Validated @RequestBody LocalLoginRequest dto, HttpServletResponse response) {

		LocalLoginResponse loginResponse = authService.login(dto);

		String refreshToken = authService.createRefreshToken(loginResponse.email());
		refreshTokenStore.save(loginResponse.email(), refreshToken, CACHE_TTL_SECONDS);

		ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
			.httpOnly(true)
			.secure(true)
			.sameSite("None")
			.path("/")
			.maxAge(CACHE_TTL_SECONDS)
			.build();

		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

		return new RsData<>(200, loginResponse);
	}

	@PostMapping("/logout")
	public RsData<Void> logout(
		@AuthenticationPrincipal UserDetails userDetails, HttpServletResponse response) {

		authService.logout();

		ResponseCookie expiredCookie = ResponseCookie.from("refreshToken", "")
			.httpOnly(true)
			.secure(true)
			.sameSite("None")
			.path("/")
			.maxAge(0)
			.build();

		response.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());

		return new RsData<>(200, null);
	}

	@PostMapping("/refresh")
	public RsData<TokenRefreshResponse> refresh(
		@CookieValue(value = "refreshToken", required = false) String refreshToken) {

		if (refreshToken == null) {
			throw ErrorCode.AUTH_TOKEN_INVALID.serviceException("Refresh token cookie missing");
		}

		TokenRefreshResponse response = authService.refresh(refreshToken);
		return new RsData<>(200, response);
	}

	@GetMapping("/me")
	public RsData<GetCurrentUserInfoResponse> me(
		@LoginUser User user) {

		GetCurrentUserInfoResponse response = authService.getMe(user);

		return new RsData<>(200, response);
	}
}
