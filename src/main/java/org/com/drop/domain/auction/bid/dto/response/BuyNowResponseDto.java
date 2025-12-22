package org.com.drop.domain.auction.bid.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record BuyNowResponseDto(

	Long auctionId,
	String auctionStatus,
	Long winnerId,
	Long finalPrice,
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
	LocalDateTime winTime
) {
}
