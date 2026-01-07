package org.com.drop.domain.user.dto;

import java.time.LocalDateTime;

import org.com.drop.domain.user.entity.User;

public record UpdateProfileResponse(
	Long userId,
	String nickname,
	String profileImageUrl,
	LocalDateTime updatedAt
) {
	public static UpdateProfileResponse of(
		User user,
		String profileImageUrl
	) {
		return new UpdateProfileResponse(
			user.getId(),
			user.getNickname(),
			profileImageUrl,
			LocalDateTime.now()
		);
	}
}
