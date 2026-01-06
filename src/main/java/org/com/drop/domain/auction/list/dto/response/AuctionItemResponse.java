package org.com.drop.domain.auction.list.dto.response;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.com.drop.domain.auction.list.repository.AuctionListRepositoryCustom.AuctionItemDto;

/**
 * 경매 리스트 아이템 응답 DTO
 */
public record AuctionItemResponse(
	Long auctionId,
	Long productId,
	String name,
	String imageUrl,
	String status,
	String category,
	String subCategory,
	Integer startPrice,
	Integer currentHighestBid,
	Integer bidCount,
	Integer bookmarkCount,
	LocalDateTime endAt,
	Long remainingTimeSeconds,
	Boolean isBookmarked
) {

	public static AuctionItemResponse from(
		AuctionItemDto dto,
		Boolean isBookmarked,
		String imageUrl
	) {
		long remainingSeconds = ChronoUnit.SECONDS.between(
			LocalDateTime.now(),
			dto.getEndAt()
		);

		return new AuctionItemResponse(
			dto.getAuctionId(),
			dto.getProductId(),
			dto.getName(),
			imageUrl,
			dto.getStatus().name(),
			dto.getCategory().name(),
			dto.getSubCategory() != null
				? dto.getSubCategory().name()
				: null,
			dto.getStartPrice(),
			dto.getCurrentHighestBid() != null
				? dto.getCurrentHighestBid()
				: dto.getStartPrice(),
			dto.getBidCount() != null ? dto.getBidCount() : 0,
			dto.getBookmarkCount() != null ? dto.getBookmarkCount() : 0,
			dto.getEndAt(),
			Math.max(0, remainingSeconds),
			isBookmarked
		);
	}
}
