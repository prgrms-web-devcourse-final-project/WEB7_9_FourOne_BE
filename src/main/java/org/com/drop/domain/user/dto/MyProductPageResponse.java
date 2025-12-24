package org.com.drop.domain.user.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MyProductPageResponse(
	int currentPage,
	int totalPages,
	long totalElements,
	List<MyProductResponse> products
) {
	public record MyProductResponse(
		Long auctionId,
		Long productId,
		String name,
		String imageUrl,
		String status, // PENDING, SCHEDULED, LIVE, ENDED, CANCELLED
		int currentHighestBid,
		int startPrice,
		LocalDateTime endAt,
		long bookmarkCount,
		long bidCount,
		long remainingTimeSeconds
	) { }
}
