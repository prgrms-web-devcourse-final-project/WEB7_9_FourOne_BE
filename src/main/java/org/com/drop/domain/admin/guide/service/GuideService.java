package org.com.drop.domain.admin.guide.service;

import java.util.List;

import org.com.drop.domain.admin.guide.dto.GuideDto;
import org.com.drop.domain.admin.guide.entity.Guide;
import org.com.drop.domain.admin.guide.repository.GuideRepository;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class GuideService {
	private final GuideRepository guideRepository;

	public List<GuideDto> getGuide() {
		List<Guide> guides = guideRepository.findAllByOrderByIdAsc();
		List<GuideDto> guideDtos = guides.stream().map(g -> new GuideDto(g.getId(), g.getContent())).toList();
		return guideDtos;
	}
}
