package org.com.drop.domain.auction.bid.dto.response;

import java.time.LocalDateTime;

public record BuyNowResponseDto(

	Long auctionId,
	String auctionStatus,
	Long winnerId,
	Integer finalPrice,
	LocalDateTime winTime
	// Long paymentRequestId,
	// String paymentStatus
) {
}
