package org.com.drop.domain.auction.bid.bidevent;

public record BidSuccessEvent(
	Long auctionId,
	Long newhighestBid) {
}
