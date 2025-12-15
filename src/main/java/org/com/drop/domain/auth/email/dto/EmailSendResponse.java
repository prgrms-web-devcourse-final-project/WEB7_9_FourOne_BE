package org.com.drop.domain.auth.email.dto;

import java.time.LocalDateTime;

public record EmailSendResponse(
	LocalDateTime sentAt
) {
	public static EmailSendResponse now() {
		return new EmailSendResponse(LocalDateTime.now());
	}
}
