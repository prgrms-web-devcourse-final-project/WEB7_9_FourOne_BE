package org.com.drop.domain.auction.product.qna.dto;

import jakarta.validation.constraints.NotBlank;

public record ProductQnAAnswerRequest(
	@NotBlank(message = "PRODUCT_INVALID_ANSWER")
	String answer
) {
}
