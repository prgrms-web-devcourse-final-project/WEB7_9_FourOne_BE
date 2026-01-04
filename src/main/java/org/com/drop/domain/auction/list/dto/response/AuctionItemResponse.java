package org.com.drop.domain.auction.list.dto.response;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.com.drop.domain.auction.list.repository.AuctionListRepositoryCustom.AuctionItemDto;

import lombok.Builder;
import lombok.Getter;

/**
 * 경매 리스트 아이템 응답 DTO
 */
@Getter
@Builder
public class AuctionItemResponse {

	private final Long auctionId;
	private final Long productId;

	private final String name;
	private final String imageUrl;

	private final String status;
	private final String category;
	private final String subCategory;

	private final Integer startPrice;
	private final Integer currentHighestBid;

	private final Integer bidCount;
	private final Integer bookmarkCount;

	private final LocalDateTime endAt;
	private final Long remainingTimeSeconds;

	private final Boolean isBookmarked;

	public static AuctionItemResponse from(
		AuctionItemDto dto,
		Boolean isBookmarked
	) {
		long remainingSeconds = ChronoUnit.SECONDS.between(
			LocalDateTime.now(),
			dto.getEndAt()
		);

		return AuctionItemResponse.builder()
			.auctionId(dto.getAuctionId())
			.productId(dto.getProductId())

			.name(dto.getName())
			.imageUrl(
				dto.getImageUrl() != null && !dto.getImageUrl().isBlank()
					? dto.getImageUrl()
					: getDefaultImageUrl()
			)

			.status(dto.getStatus().name())
			.category(dto.getCategory().name())
			.subCategory(
				dto.getSubCategory() != null
					? dto.getSubCategory().name()
					: null
			)

			.startPrice(dto.getStartPrice())
			.currentHighestBid(
				dto.getCurrentHighestBid() != null
					? dto.getCurrentHighestBid()
					: dto.getStartPrice()
			)

			.bidCount(dto.getBidCount() != null ? dto.getBidCount() : 0)
			.bookmarkCount(dto.getBookmarkCount() != null ? dto.getBookmarkCount() : 0)

			.endAt(dto.getEndAt())
			.remainingTimeSeconds(Math.max(0, remainingSeconds))

			.isBookmarked(isBookmarked)
			.build();
	}

	private static String getDefaultImageUrl() {
		return "https://drop-auction-bucket.s3.amazonaws.com/default/auction-default.jpg";
	}
}
