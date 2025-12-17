package org.com.drop.domain.user.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.com.drop.domain.auction.product.entity.BookMark;

public record MyBookmarkPageResponse(
	int page,
	long total,
	List<MyBookmarkResponse> bookmarks
) {
	public record MyBookmarkResponse(
		Long id,
		Long productId,
		String title,
		String productImageUrl,
		LocalDateTime bookmarkedAt
	) {
		public static MyBookmarkResponse of(BookMark bm, String imageUrl) {
			return new MyBookmarkResponse(
				bm.getId(),
				bm.getProduct().getId(),
				bm.getProduct().getName(),
				imageUrl,
				bm.getCreatedAt()
			);
		}
	}
}
