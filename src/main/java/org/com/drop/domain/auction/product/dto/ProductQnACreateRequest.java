package org.com.drop.domain.auction.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductQnACreateRequest(
	@NotBlank
	@Size(max = 500)
	String question
) {
}
