package org.com.drop.domain.auth.email.dto;

import java.time.LocalDateTime;

public record EmailVerifyResponse(
	LocalDateTime verifiedAt
) {
	public static EmailVerifyResponse now() {
		return new EmailVerifyResponse(LocalDateTime.now());
	}
}
