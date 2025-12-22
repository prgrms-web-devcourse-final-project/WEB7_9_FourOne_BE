package org.com.drop.domain.auction.bid.bidEvent;

public record BidSuccessEvent(
	Long auctionId,
	Long newhighestBid) {
}
