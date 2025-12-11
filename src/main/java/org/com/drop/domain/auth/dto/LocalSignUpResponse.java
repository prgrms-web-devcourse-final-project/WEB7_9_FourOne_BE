package org.com.drop.domain.auth.dto;

import org.com.drop.domain.auth.jwt.JwtProvider;

public record LocalSignUpResponse(
	Long userId,
	String email,
	String nickname,
	String accessToken,
	String refreshToken
) {
	public static LocalSignUpResponse of(Long userId, String email, String nickname, JwtProvider jwtProvider) {
		String accessToken = jwtProvider.createAccessToken(email);
		String refreshToken = jwtProvider.createRefreshToken(email);
		return new LocalSignUpResponse(userId, email, nickname, accessToken, refreshToken);
	}
}
