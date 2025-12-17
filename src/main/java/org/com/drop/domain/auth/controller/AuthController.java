package org.com.drop.domain.auth.controller;

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
import org.com.drop.domain.user.entity.User;
import org.com.drop.global.rsdata.RsData;
import org.com.drop.global.security.auth.LoginUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	static final int CACHE_TTL_SECONDS = 7 * 24 * 60 * 60;
	private final AuthService authService;

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
		return new RsData<>(response);
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
		@Validated @RequestBody LocalLoginRequest dto,
		HttpServletResponse response
	) {
		LocalLoginResponse loginResponse = authService.login(dto);

		String refreshToken = authService.createRefreshToken(loginResponse.email());

		ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
			.httpOnly(true)
			.path("/")
			.maxAge(CACHE_TTL_SECONDS)
			.build();

		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

		return new RsData<>(loginResponse);
	}

	@PostMapping("/logout")
	public RsData<Void> logout(
		@LoginUser User user,
		HttpServletResponse response
	) {
		authService.logout();

		response.addHeader(
			HttpHeaders.SET_COOKIE,
			ResponseCookie.from("refreshToken", "")
				.httpOnly(true)
				.path("/")
				.maxAge(0)
				.build()
				.toString()
		);

		return new RsData<>(null);
	}

	@PostMapping("/refresh")
	public RsData<TokenRefreshResponse> refresh(HttpServletRequest request) {

		String refreshToken = (String)request.getAttribute("validRefreshToken");

		TokenRefreshResponse response = authService.refresh(refreshToken);
		return new RsData<>(response);
	}
}
