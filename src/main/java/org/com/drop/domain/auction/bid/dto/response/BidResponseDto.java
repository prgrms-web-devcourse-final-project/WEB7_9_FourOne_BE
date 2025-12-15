package org.com.drop.domain.auction.bid.dto.response;

import java.time.LocalDateTime;


public record BidResponseDto(
	Long auctionId,
	boolean isHighestBidder,
	Long currentHighestBid,
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
