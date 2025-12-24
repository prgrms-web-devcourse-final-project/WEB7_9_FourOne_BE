package org.com.drop.domain.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
	@Size(min = 3, max = 10)
	String nickname,

	String profileImageKey
) { }
