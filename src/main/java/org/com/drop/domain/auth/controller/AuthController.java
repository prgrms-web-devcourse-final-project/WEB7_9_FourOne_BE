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
import org.com.drop.domain.user.entity.User;
import org.com.drop.global.rsdata.RsData;
import org.com.drop.global.security.auth.LoginUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	static final int CACHE_TTL_SECONDS = 7 * 24 * 60 * 60;

	private <T> RsData<T> createSuccessRsData(int status, T data) {
		return new RsData<>(status, data);
	}

	@PostMapping("/local/signup")
	public ResponseEntity<RsData<LocalSignUpResponse>> signup(
		@Valid @RequestBody LocalSignUpRequest dto) {

		LocalSignUpResponse response = authService.signup(dto);
		RsData<LocalSignUpResponse> rsData = createSuccessRsData(201, response);

		return ResponseEntity.created(null).body(rsData);
	}

	@PostMapping("/email/send-code")
	public ResponseEntity<RsData<EmailSendResponse>> sendVerificationCode(
		@Valid @RequestBody EmailSendRequest dto) {

		EmailSendResponse response = authService.sendVerificationCode(dto.email());
		RsData<EmailSendResponse> rsData = createSuccessRsData(202, response);

		return ResponseEntity.status(HttpStatus.ACCEPTED).body(rsData);
	}

	@PostMapping("/email/verify-code")
	public ResponseEntity<RsData<EmailVerifyResponse>> verifyCode(
		@Valid @RequestBody EmailVerifyRequest dto) {

		EmailVerifyResponse response = authService.verifyCode(dto.email(), dto.code());
		RsData<EmailVerifyResponse> rsData = createSuccessRsData(200, response);

		return ResponseEntity.ok(rsData);
	}

	@PostMapping("/delete")
	public RsData<UserDeleteResponse> deleteAccount(
		@LoginUser User user,
		@Validated @RequestBody UserDeleteRequest request) {

		UserDeleteResponse response = authService.deleteAccount(user, request);

		return createSuccessRsData(200, response);
	}

	@PostMapping("/login")
	public ResponseEntity<RsData<LocalLoginResponse>> login(
		@Validated @RequestBody LocalLoginRequest dto) {

		LocalLoginResponse response = authService.login(dto);

		String refreshToken = authService.createRefreshToken(response.email());

		ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
			.httpOnly(true)
			.path("/")
			.maxAge(CACHE_TTL_SECONDS)
			.build();

		RsData<LocalLoginResponse> body = createSuccessRsData(200, response);

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, cookie.toString())
			.body(body);
	}

	@PostMapping("/logout")
	public ResponseEntity<RsData<Void>> logout(
		@AuthenticationPrincipal UserDetails userDetails) {

		authService.logout();

		ResponseCookie expiredCookie = ResponseCookie.from("refreshToken", "")
			.httpOnly(true)
			.path("/")
			.maxAge(0)
			.build();

		RsData<Void> body = createSuccessRsData(200, null);

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
			.body(body);
	}

	@PostMapping("/refresh")
	public RsData<TokenRefreshResponse> refresh(HttpServletRequest request) {

		String refreshToken = (String) request.getAttribute("validRefreshToken");

		TokenRefreshResponse response = authService.refresh(refreshToken);

		return createSuccessRsData(200, response);
	}

	@GetMapping("/me")
	public RsData<GetCurrentUserInfoResponse> me(
		@LoginUser User user) {

		GetCurrentUserInfoResponse response = authService.getMe(user);

		return createSuccessRsData(200, response);
	}
}
