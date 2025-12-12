package org.com.drop.domain.auction.product.qna.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductQnAAnswerRequest(
	@NotBlank
	@Size(max = 500)
	String answer
) {
}
