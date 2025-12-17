package org.com.drop.domain.auth.service;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

import org.com.drop.domain.auth.dto.LocalLoginRequest;
import org.com.drop.domain.auth.dto.LocalLoginResponse;
import org.com.drop.domain.auth.dto.LocalSignUpRequest;
import org.com.drop.domain.auth.dto.LocalSignUpResponse;
import org.com.drop.domain.auth.dto.TokenRefreshResponse;
import org.com.drop.domain.auth.dto.UserDeleteRequest;
import org.com.drop.domain.auth.dto.UserDeleteResponse;
import org.com.drop.domain.auth.email.dto.EmailSendResponse;
import org.com.drop.domain.auth.email.dto.EmailVerifyResponse;
import org.com.drop.domain.auth.email.service.EmailService;
import org.com.drop.domain.auth.jwt.JwtProvider;
import org.com.drop.domain.auth.store.RefreshTokenStore;
import org.com.drop.domain.auth.store.VerificationCodeStore;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.entity.User.LoginType;
import org.com.drop.domain.user.entity.User.UserRole;
import org.com.drop.domain.user.repository.UserRepository;
import org.com.drop.global.exception.ErrorCode;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenStore refreshTokenStore;
	private final JwtProvider jwtProvider;
	private final EmailService emailService;
	private final VerificationCodeStore verificationCodeStore;
	private static final int VERIFICATION_CODE_MIN = 100_000;
	private static final int VERIFICATION_CODE_MAX = 999_999;
	@Transactional
	public LocalSignUpResponse signup(LocalSignUpRequest dto) {

		if (!verificationCodeStore.isVerified(dto.email())) {
			throw ErrorCode.AUTH_CODE_EXPIRED
				.serviceException("email=%s", dto.email());
		}

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
		verificationCodeStore.removeVerifiedMark(dto.email());

		return LocalSignUpResponse.of(saved.getId(), saved.getEmail(), saved.getNickname());
	}

	public EmailSendResponse sendVerificationCode(String email) {
		if (userRepository.existsByEmail(email)) {
			throw ErrorCode.AUTH_DUPLICATE_EMAIL
				.serviceException("email=%s", email);
		}

		String code = generateRandomCode();

		try {
			verificationCodeStore.saveCode(email, code);
			emailService.sendVerificationEmail(email, code);
		} catch (MailException e) {
			verificationCodeStore.removeCode(email);

			throw ErrorCode.AUTH_EMAIL_SEND_FAILED
				.serviceException("mail send failed: email=%s", email);

		} catch (Exception e) {
			throw ErrorCode.AUTH_EMAIL_SEND_FAILED
				.serviceException("verification email process failed: email=%s", email);
		}

		return EmailSendResponse.now();
	}

	public String createRefreshToken(String email) {
		return jwtProvider.createRefreshToken(email);
	}

	public EmailVerifyResponse verifyCode(String email, String submittedCode) {

		String storedCode = verificationCodeStore.getCode(email);

		if (storedCode == null) {
			throw ErrorCode.AUTH_CODE_EXPIRED
				.serviceException("인증 코드가 만료되었거나 존재하지 않습니다. email=%s", email);
		}

		if (!storedCode.equals(submittedCode)) {
			throw ErrorCode.AUTH_CODE_MISMATCH
				.serviceException("인증 코드가 일치하지 않습니다. email=%s", email);
		}

		verificationCodeStore.removeCode(email);
		verificationCodeStore.markAsVerified(email);

		return EmailVerifyResponse.now();
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

		deleteRefreshTokenAsync(user.getEmail());

		return new UserDeleteResponse(user.getDeletedAt().toString());
	}

	@Async
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void deleteRefreshTokenAsync(String email) {
		refreshTokenStore.delete(email);
	}

	private boolean userHasActiveAuctionsOrTrades(User user) {
		// TODO: 진행 중인 경매나 입찰이 있는 경우 처리
		return false;
	}

	@Transactional(readOnly = true)
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


	private String generateRandomCode() {
		int code = ThreadLocalRandom.current()
			.nextInt(VERIFICATION_CODE_MIN, VERIFICATION_CODE_MAX + 1);
		return String.valueOf(code);
	}
}
