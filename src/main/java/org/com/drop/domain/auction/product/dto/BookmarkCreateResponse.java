package org.com.drop.domain.auction.product.dto;

import java.time.LocalDateTime;

import org.com.drop.domain.auction.product.entity.BookMark;

public record BookmarkCreateResponse(
	Long bookmarkedId,
	Long productId,
	LocalDateTime bookmarkedAt
) {
	public BookmarkCreateResponse(BookMark bookMark) {
		this(
			bookMark.getId(),
			bookMark.getProduct().getId(),
			bookMark.getCreatedAt()
		);
	}
}
