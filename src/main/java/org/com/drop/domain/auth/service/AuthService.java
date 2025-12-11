package org.com.drop.domain.auth.service;

import java.time.LocalDateTime;

import org.com.drop.domain.auth.dto.LocalSignUpRequest;
import org.com.drop.domain.auth.dto.LocalSignUpResponse;
import org.com.drop.domain.auth.jwt.JwtProvider;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.entity.User.LoginType;
import org.com.drop.domain.user.entity.User.UserRole;
import org.com.drop.domain.user.repository.UserRepository;
import org.com.drop.global.exception.ErrorCode;
import org.com.drop.global.exception.ServiceException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

// ToDo: 이메일 인증, 검증 관련 로직 추가 후 트랜잭션 처리
@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProvider jwtProvider;

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

		return LocalSignUpResponse.of(saved.getId(), saved.getEmail(), saved.getNickname(), jwtProvider);
	}
}
