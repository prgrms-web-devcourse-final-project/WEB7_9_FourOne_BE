package org.com.drop.domain.auction.product.qna.dto;

import jakarta.validation.constraints.NotBlank;

public record ProductQnACreateRequest(
	@NotBlank(message = "PRODUCT_INVALID_QUESTION")
	String question
) {
}
