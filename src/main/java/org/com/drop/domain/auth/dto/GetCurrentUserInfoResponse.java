package org.com.drop.domain.auth.dto;

import org.com.drop.domain.user.entity.User;

public record GetCurrentUserInfoResponse(
	Long userId,
	String email,
	String nickname
) {
	public static GetCurrentUserInfoResponse of(User user) {
		return new GetCurrentUserInfoResponse(user.getId(), user.getEmail(), user.getNickname());
	}
}
