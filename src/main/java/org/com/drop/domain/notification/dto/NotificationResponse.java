package org.com.drop.domain.notification.dto;

import java.time.LocalDateTime;

import org.com.drop.domain.notification.entity.Notification;

public record NotificationResponse(
	Long id,
	Long userId,
	String message,
	LocalDateTime sendAt,
	LocalDateTime readAt
) {
	public NotificationResponse(Notification notification) {
		this(
			notification.getId(),
			notification.getUser().getId(),
			notification.getMessage(),
			notification.getSendAt(),
			notification.getReadAt()
		);
	}
}
