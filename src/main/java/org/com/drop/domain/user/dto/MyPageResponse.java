package org.com.drop.domain.user.dto;

import java.time.LocalDateTime;

import org.com.drop.domain.user.entity.User;

public record MyPageResponse(
	Long userId,
	String email,
	String nickname,
	String profileImageUrl,
	LocalDateTime createdAt
) {
	public static MyPageResponse of(User user, String imageUrl) {
		return new MyPageResponse(
			user.getId(),
			user.getEmail(),
			user.getNickname(),
			imageUrl,
			user.getCreatedAt()
		);
	}
}
