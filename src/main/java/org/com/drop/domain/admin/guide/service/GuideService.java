package org.com.drop.domain.admin.guide.service;

import java.util.List;

import org.com.drop.domain.admin.guide.dto.GuideCreateRequest;
import org.com.drop.domain.admin.guide.dto.GuideDto;
import org.com.drop.domain.admin.guide.entity.Guide;
import org.com.drop.domain.admin.guide.repository.GuideRepository;
import org.com.drop.domain.user.entity.User;
import org.com.drop.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GuideService {
	private final GuideRepository guideRepository;

	public void validUser(User actor) {
		if (actor.getRole() != User.UserRole.ADMIN) {
			throw ErrorCode.GUIDE_UNAUTHORIZED
				.serviceException(
					"가이드 수정 권한이 없습니다.", actor.getId()
				);
		}
	}

	public List<GuideDto> getGuide() {
		List<Guide> guides = guideRepository.findAllByOrderByIdAsc();
		List<GuideDto> guideDtos = guides.stream().map(g -> new GuideDto(g.getId(), g.getContent())).toList();
		return guideDtos;
	}

	public Guide getGuideById(Long id) {
		return guideRepository.findById(id).orElseThrow(() ->
			ErrorCode.GUIDE_NOT_FOUND
				.serviceException("guideId=%d", id)
		);
	}

	@Transactional
	public Guide addGuide(GuideCreateRequest request, User actor) {
		validUser(actor);
		Guide guide = new Guide(request.content());
		return guideRepository.save(guide);
	}

	public Guide updateGuide(GuideCreateRequest request, Long guideId, User actor) {
		validUser(actor);
		Guide guide = getGuideById(guideId);
		guide.setContent(request.content());
		return guideRepository.save(guide);
	}
}
