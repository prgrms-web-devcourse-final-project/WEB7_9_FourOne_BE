package org.com.drop.domain.auth.dto;

import lombok.Builder;

@Builder
public record TokenRefreshResponse(
	String accessToken,
	long expiresIn
) { }
