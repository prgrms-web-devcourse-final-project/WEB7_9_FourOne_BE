package org.com.drop.domain.auction.auction.dto;

import java.time.LocalDateTime;

import org.com.drop.domain.auction.auction.entity.Auction;

public record AuctionCreateResponse(
	Long auctionId,
	Long product_id,
	Integer startPrice,
	Integer buyNowPrice,
	Integer midBidStep,
	LocalDateTime startAt,
	LocalDateTime endAt
) {
	public AuctionCreateResponse(Auction auction) {
		this(
			auction.getId(),
			auction.getProduct().getId(),
			auction.getStartPrice(),
			auction.getBuyNowPrice(),
			auction.getMidBidStep(),
			auction.getStartAt(),
			auction.getEndAt()
		);
	}
}
