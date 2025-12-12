package org.com.drop.domain.auction.product.dto;

import java.time.LocalDateTime;

public record ProductQnAAnswerResponse(
	Long qnaId,
	Long questionerId,
	String question,
	LocalDateTime questionedAt
) {
}
