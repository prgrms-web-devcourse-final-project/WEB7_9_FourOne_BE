package org.com.drop.domain.auction.product.qna.dto;

import java.time.LocalDateTime;

import org.com.drop.domain.auction.product.qna.entity.Question;

public record ProductQnaCreateResponse(
	Long qnaId,
	Long questionerId,
	String question,
	LocalDateTime questionedAt
) {
	public ProductQnaCreateResponse(Question question) {
		this(
			question.getId(),
			question.getQuestioner().getId(),
			question.getQuestion(),
			question.getCreatedAt()
		);
	}
}
