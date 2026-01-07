package org.com.drop.domain.admin.guide.dto;

import org.com.drop.domain.admin.guide.entity.Guide;

public record GuideDto(
	Long id,
	String content
) {
	public GuideDto(Guide guide) {
		this(
			guide.getId(),
			guide.getContent()
		);
	}
}
