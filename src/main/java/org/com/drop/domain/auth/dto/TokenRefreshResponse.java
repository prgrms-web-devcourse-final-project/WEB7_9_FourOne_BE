package org.com.drop.domain.auth.dto;

public record TokenRefreshResponse(
	String accessToken,
	long expiresIn
) {
}
