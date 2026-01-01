package org.com.drop.domain.admin.guide.controller;

import java.util.List;

import org.com.drop.domain.admin.guide.dto.GuideCreateRequest;
import org.com.drop.domain.admin.guide.dto.GuideDto;
import org.com.drop.domain.admin.guide.dto.ProductHelpResponse;
import org.com.drop.domain.admin.guide.entity.Guide;
import org.com.drop.domain.admin.guide.service.GuideService;
import org.com.drop.domain.user.entity.User;
import org.com.drop.global.rsdata.RsData;
import org.com.drop.global.security.auth.LoginUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
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

	@PostMapping
	public RsData<GuideDto> createGuide(
		@Valid @RequestBody GuideCreateRequest request,
		@LoginUser User actor
	) {
		Guide guide = guideService.addGuide(request, actor);
		return new RsData<>(
			new GuideDto(guide)
		);
	}

	@PutMapping("/{guideId}")
	public RsData<GuideDto> updateGuide(
		@PathVariable Long guideId,
		@LoginUser User actor,
		@Valid @RequestBody GuideCreateRequest request
	) {
		Guide guide = guideService.updateGuide(request, guideId, actor);
		return new RsData<>(
			new GuideDto(guide)
		);
	}
}
