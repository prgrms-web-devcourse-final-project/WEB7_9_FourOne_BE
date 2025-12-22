package org.com.drop.domain.auction.bid.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BidRequestDto(

	@NotNull(message = "입찰 금액은 필수입니다.")
	@Min(value = 1, message = "입찰 금액은 최소 1원 이상이어야 합니다.")
	Long bidAmount //입찰금액
) {
}
