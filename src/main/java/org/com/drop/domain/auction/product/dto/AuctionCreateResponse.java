package org.com.drop.domain.auction.product.dto;

import java.time.LocalDateTime;

import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.product.entity.Product;

public record AuctionCreateResponse(
	Long auctionId,
	Long productId,
	Auction.AuctionStatus status,
	LocalDateTime startAt,
	LocalDateTime endAt,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
	public AuctionCreateResponse(Product product, Auction auction) {
		this(
			auction.getId(),
			product.getId(),
			auction.getStatus(),
			auction.getStartAt(),
			auction.getEndAt(),
			product.getCreatedAt(),
			product.getUpdatedAt()
		);
	}
}
