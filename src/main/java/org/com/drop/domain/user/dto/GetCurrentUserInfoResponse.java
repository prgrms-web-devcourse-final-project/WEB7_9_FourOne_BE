package org.com.drop.domain.user.dto;

import java.time.LocalDateTime;

import org.com.drop.domain.user.entity.User;

public record GetCurrentUserInfoResponse(
	Long userId,
	String email,
	String nickname,
	String profileImageUrl,
	LocalDateTime createdAt
) {
	public static GetCurrentUserInfoResponse of(User user) {
		return new GetCurrentUserInfoResponse(
			user.getId(),
			user.getEmail(),
			user.getNickname(),
			user.getUserProfile(),
			user.getCreatedAt()
		);
	}
}
