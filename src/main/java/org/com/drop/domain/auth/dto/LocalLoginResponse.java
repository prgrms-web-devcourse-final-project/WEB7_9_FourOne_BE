package org.com.drop.domain.auth.dto;

public record LocalLoginResponse(
	Long userId,
	String email,
	String nickname,
	String accessToken,
	Long expiresIn
) { }
