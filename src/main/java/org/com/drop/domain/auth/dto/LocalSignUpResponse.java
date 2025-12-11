package org.com.drop.domain.auth.dto;

public record LocalSignUpResponse(
	Long userId,
	String email,
	String nickname
) {
	public static LocalSignUpResponse of(Long userId, String email, String nickname) {
		return new LocalSignUpResponse(userId, email, nickname);
	}
}
