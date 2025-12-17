package org.com.drop.domain.admin.guide.controller;

import java.util.List;

import org.com.drop.domain.admin.guide.dto.GuideDto;
import org.com.drop.domain.admin.guide.dto.ProductHelpResponse;
import org.com.drop.domain.admin.guide.service.GuideService;
import org.com.drop.global.rsdata.RsData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/help")
public class GuideController {

	private final GuideService guideService;

	@GetMapping
	public RsData<ProductHelpResponse> getGuide() {
		List<GuideDto> guides = guideService.getGuide();
		return new RsData<>(
			new ProductHelpResponse(guides)
		);
	}
}
