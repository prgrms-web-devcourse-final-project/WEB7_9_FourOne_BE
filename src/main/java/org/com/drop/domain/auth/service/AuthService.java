package org.com.drop.domain.auth.service;

import java.time.LocalDateTime;

import org.com.drop.domain.auth.dto.GetCurrentUserInfoResponse;
import org.com.drop.domain.auth.dto.LocalLoginRequest;
import org.com.drop.domain.auth.dto.LocalLoginResponse;
import org.com.drop.domain.auth.dto.LocalSignUpRequest;
import org.com.drop.domain.auth.dto.LocalSignUpResponse;
import org.com.drop.domain.auth.dto.TokenRefreshResponse;
import org.com.drop.domain.auth.dto.UserDeleteRequest;
import org.com.drop.domain.auth.dto.UserDeleteResponse;
import org.com.drop.domain.auth.jwt.JwtProvider;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.entity.User.LoginType;
import org.com.drop.domain.user.entity.User.UserRole;
import org.com.drop.domain.user.repository.UserRepository;
import org.com.drop.global.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// ToDo: 이메일 인증, 검증 관련 로직 추가 후 트랜잭션 확인
@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenStore refreshTokenStore;
	@Getter
	private final JwtProvider jwtProvider;

	@Transactional
	public LocalSignUpResponse signup(LocalSignUpRequest dto) {

		if (userRepository.existsByEmail(dto.email())) {
			throw ErrorCode.AUTH_DUPLICATE_EMAIL
				.serviceException("email=%s", dto.email());
		}

		if (userRepository.existsByNickname(dto.nickname())) {
			throw ErrorCode.AUTH_DUPLICATE_NICKNAME
				.serviceException("nickname=%s", dto.nickname());
		}

		User user = User.builder()
			.email(dto.email())
			.password(passwordEncoder.encode(dto.password()))
			.nickname(dto.nickname())
			.loginType(LoginType.LOCAL)
			.role(UserRole.USER)
			.createdAt(LocalDateTime.now())
			.penaltyCount(0)
			.build();

		User saved = userRepository.save(user);

		return LocalSignUpResponse.of(saved.getId(), saved.getEmail(), saved.getNickname());
	}

	@Transactional
	public UserDeleteResponse deleteAccount(User user, UserDeleteRequest request) {

		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw ErrorCode.AUTH_PASSWORD_MISMATCH
				.serviceException("userId=%d", user.getId());
		}

		if (userHasActiveAuctionsOrTrades(user)) {
			throw ErrorCode.USER_HAS_ACTIVE_AUCTIONS
				.serviceException("userId=%d", user.getId());
		}

		user.markAsDeleted();
		userRepository.save(user);

		refreshTokenStore.delete(user.getEmail());

		return new UserDeleteResponse(user.getDeletedAt().toString());
	}

	private boolean userHasActiveAuctionsOrTrades(User user) {
		// TODO: 진행 중인 경매나 입찰이 있는 경우 처리
		return false;
	}

	public LocalLoginResponse login(LocalLoginRequest dto) {
		User user = userRepository.findByEmail(dto.email())
			.orElseThrow(() ->
				ErrorCode.AUTH_UNAUTHORIZED
					.serviceException("email=%s", dto.email())
			);

		if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
			throw ErrorCode.AUTH_UNAUTHORIZED
				.serviceException("userId=%d (password mismatch)", user.getId());
		}

		String accessToken = jwtProvider.createAccessToken(user.getEmail());
		long expiresIn = jwtProvider.getAccessTokenValidityInSeconds();

		return new LocalLoginResponse(
			user.getId(),
			user.getEmail(),
			user.getNickname(),
			accessToken,
			expiresIn
		);
	}

	@Transactional
	public void logout() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()) {
			throw ErrorCode.AUTH_UNAUTHORIZED
				.serviceException("No authenticated user found for logout.");
		}

		String email = authentication.getName();

		refreshTokenStore.delete(email);
	}

	public TokenRefreshResponse refresh(String refreshToken) {
		String email;

		try {
			email = jwtProvider.getUsername(refreshToken);
		} catch (Exception e) {
			throw ErrorCode.AUTH_TOKEN_INVALID
				.serviceException("refreshToken parsing failed");
		}

		if (!refreshTokenStore.exists(email, refreshToken)) {
			throw ErrorCode.AUTH_TOKEN_INVALID
				.serviceException(
					"refreshToken not found in store, email=%s",
					email
				);
		}

		String newAccessToken = jwtProvider.createAccessToken(email);

		long expiresIn = jwtProvider.getAccessTokenValidityInSeconds();

		return new TokenRefreshResponse(newAccessToken, expiresIn);
	}

	public GetCurrentUserInfoResponse getMe(User user) {
		return GetCurrentUserInfoResponse.of(user);
	}
}
