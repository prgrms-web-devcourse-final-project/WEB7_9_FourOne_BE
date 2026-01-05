package org.com.drop.domain.admin.guide.dto;

import jakarta.validation.constraints.NotBlank;

public record GuideCreateRequest(
	@NotBlank(message = "GUIDE_INVALID_CONTENT")
	String content
) {
}
