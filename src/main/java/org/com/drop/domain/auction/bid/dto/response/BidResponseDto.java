package org.com.drop.domain.auction.bid.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public record BidResponseDto(
	Long auctionId,
	boolean isHighestBidder,
	Long currentHighestBid,
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
	LocalDateTime bidTime
) {
	public static BidResponseDto of(
		Long auctionId,
		boolean isHighestBidder,
		Long currentHighestBid,
		LocalDateTime bidTime
	) {
		return new BidResponseDto(auctionId, isHighestBidder, currentHighestBid, bidTime);
	}
}
