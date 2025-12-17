package org.com.drop.domain.user.dto;

import java.time.LocalDateTime;

public record MyAuctionListResponse(
	Long auctionId,
	Long productId,
	String productName,
	String productImageUrl,
	long myBid,
	Long finalBid,
	String status,
	LocalDateTime endAt
) {
}
