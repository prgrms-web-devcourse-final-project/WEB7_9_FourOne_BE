package org.com.drop.domain.auction.product.qna.dto;

import java.time.LocalDateTime;

import org.com.drop.domain.auction.product.qna.entity.Answer;

public record ProductQnAAnswerResponse(
	Long qnaId,
	Long answerId,
	Long answererId,
	String answer,
	LocalDateTime answeredAt
) {
	public ProductQnAAnswerResponse(Answer answer) {
		this(
			answer.getProduct().getId(),
			answer.getId(),
			answer.getAnswerer().getId(),
			answer.getAnswer(),
			answer.getCreatedAt()
		);
	}
}
