package org.com.drop.domain.admin.guide.dto;

import java.util.List;

public record ProductHelpResponse(
	List<GuideDto> guides
) {
}
