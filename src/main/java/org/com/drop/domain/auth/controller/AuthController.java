package org.com.drop.domain.auth.controller;

import org.com.drop.domain.auth.dto.GetCurrentUserInfoResponse;
import org.com.drop.domain.auth.dto.LocalLoginRequest;
import org.com.drop.domain.auth.dto.LocalLoginResponse;
import org.com.drop.domain.auth.dto.LocalSignUpRequest;
import org.com.drop.domain.auth.dto.LocalSignUpResponse;
import org.com.drop.domain.auth.dto.TokenRefreshResponse;
import org.com.drop.domain.auth.dto.UserDeleteRequest;
import org.com.drop.domain.auth.dto.UserDeleteResponse;
import org.com.drop.domain.auth.service.AuthService;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.com.drop.global.exception.ErrorCode;
import org.com.drop.global.exception.ServiceException;
import org.com.drop.global.rsData.RsData;
import org.springframework.http.HttpHeaders;
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

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final UserRepository userRepository;

	private <T> RsData<T> createSuccessRsData(T data) {
		return new RsData<>(
			"SUCCESS",
			"200",
			"요청을 성공적으로 처리했습니다.",
			data
		);
	}

	@PostMapping("/local/signup")
	public ResponseEntity<RsData<LocalSignUpResponse>> signup(
		@Valid @RequestBody LocalSignUpRequest dto) {

		LocalSignUpResponse response = authService.signup(dto);
		RsData<LocalSignUpResponse> rsData = createSuccessRsData(response);

		return ResponseEntity.ok(rsData);
	}

	// ToDo: Refresh token을 Redis 에 저장하는 로직 추가
	@PostMapping("/delete")
	public RsData<UserDeleteResponse> deleteAccount(
		@AuthenticationPrincipal UserDetails userDetails,
		@Validated @RequestBody UserDeleteRequest request) {

		User user = findUserFromUserDetails(userDetails);

		UserDeleteResponse response = authService.deleteAccount(user, request);

		return createSuccessRsData(response);
	}

	private User findUserFromUserDetails(UserDetails userDetails) {
		return userRepository.findByEmail(userDetails.getUsername())
			.orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));
	}

	// ToDo: Refresh token을 Redis 에 저장하는 로직 추가
	@PostMapping("/login")
	public ResponseEntity<RsData<LocalLoginResponse>> login(
		@Validated @RequestBody LocalLoginRequest dto) {

		LocalLoginResponse response = authService.login(dto);

		String refreshToken = authService.getJwtProvider().createRefreshToken(response.email());

		ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
			.httpOnly(true)
			.path("/")
			.maxAge(7 * 24 * 60 * 60)
			.build();

		RsData<LocalLoginResponse> body = createSuccessRsData(response);

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, cookie.toString())
			.body(body);
	}

	@PostMapping("/logout")
	public ResponseEntity<RsData<Void>> logout(
		@AuthenticationPrincipal UserDetails userDetails) {

		authService.logout(userDetails.getUsername());

		ResponseCookie expiredCookie = ResponseCookie.from("refreshToken", "")
			.httpOnly(true)
			.path("/")
			.maxAge(0)
			.build();

		RsData<Void> body = createSuccessRsData(null);

		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
			.body(body);
	}

	@PostMapping("/refresh")
	public RsData<TokenRefreshResponse> refresh(HttpServletRequest request) {

		String refreshToken = extractRefreshTokenFromCookie(request);

		if (refreshToken == null || refreshToken.isBlank()) {
			throw new ServiceException(ErrorCode.AUTH_TOKEN_MISSING);
		}

		TokenRefreshResponse response = authService.refresh(refreshToken);

		return createSuccessRsData(response);
	}

	private String extractRefreshTokenFromCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}

		for (Cookie cookie : cookies) {
			if ("refreshToken".equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}

	@GetMapping("/me")
	public RsData<GetCurrentUserInfoResponse> me(
		@AuthenticationPrincipal UserDetails userDetails) {

		User user = findUserFromUserDetails(userDetails);
		GetCurrentUserInfoResponse response = authService.getMe(user);

		return createSuccessRsData(response);
	}
}
