package org.com.drop.domain.user.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MyBidPageResponse(
	int currentPage,
	int totalPages,
	long totalElements,
	List<MyBidResponse> auctions
) {
	public record MyBidResponse(
		Long auctionId,
		Long productId,
		String productName,
		String productImageUrl,
		int myBid,
		Integer finalBid,
		String status,
		LocalDateTime endAt
	) { }
}
