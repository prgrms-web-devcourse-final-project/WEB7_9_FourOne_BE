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
	public static MyPageResponse of(User user) {
		return new MyPageResponse(
			user.getId(),
			user.getEmail(),
			user.getNickname(),
			user.getUserProfile(),
			user.getCreatedAt()
		);
	}
}
