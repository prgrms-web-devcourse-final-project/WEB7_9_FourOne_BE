package org.com.drop.domain.auth.service;

import java.time.LocalDateTime;

import org.com.drop.domain.auth.dto.LocalLoginRequest;
import org.com.drop.domain.auth.dto.LocalLoginResponse;
import org.com.drop.domain.auth.dto.LocalSignUpRequest;
import org.com.drop.domain.auth.dto.LocalSignUpResponse;
import org.com.drop.domain.auth.dto.UserDeleteRequest;
import org.com.drop.domain.auth.dto.UserDeleteResponse;
import org.com.drop.domain.auth.jwt.JwtProvider;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.entity.User.LoginType;
import org.com.drop.domain.user.entity.User.UserRole;
import org.com.drop.domain.user.repository.UserRepository;
import org.com.drop.global.exception.ErrorCode;
import org.com.drop.global.exception.ServiceException;
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
			throw new ServiceException(ErrorCode.AUTH_DUPLICATE_EMAIL);
		}

		if (userRepository.existsByNickname(dto.nickname())) {
			throw new ServiceException(ErrorCode.AUTH_DUPLICATE_NICKNAME);
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
			throw new ServiceException(ErrorCode.AUTH_PASSWORD_MISMATCH);
		}

		if (userHasActiveAuctionsOrTrades(user)) {
			throw new ServiceException(ErrorCode.USER_HAS_ACTIVE_AUCTIONS);
		}

		user.setDeletedAt(LocalDateTime.now());
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
			.orElseThrow(() -> new ServiceException(ErrorCode.AUTH_UNAUTHORIZED));

		if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
			throw new ServiceException(ErrorCode.AUTH_UNAUTHORIZED);
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
	public void logout(String email) {
		refreshTokenStore.delete(email);
	}


}
