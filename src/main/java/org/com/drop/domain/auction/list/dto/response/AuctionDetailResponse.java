package org.com.drop.domain.auction.list.dto.response;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.com.drop.domain.auction.bid.dto.response.BidHistoryResponse;
import org.com.drop.domain.auction.list.repository.AuctionListRepositoryCustom.AuctionDetailDto;

import lombok.Builder;
import lombok.Getter;

/**
 * 경매 상세 응답 DTO
 */
@Getter
@Builder
public class AuctionDetailResponse {
	private final Long auctionId;
	private final Long productId;
	private final Long sellerId;
	private final String sellerNickname;
	private final String name;
	private final String description;
	private final String category;
	private final String subCategory;
	private final String status;
	private final Integer startPrice;
	private final Integer buyNowPrice;
	private final Integer minBidStep;
	private final LocalDateTime startAt;
	private final LocalDateTime endAt;
	private final LocalDateTime createdAt;
	private final Integer currentHighestBid;
	private final Integer totalBidCount;
	private final Long remainingTimeSeconds;
	private final List<String> imageUrls;
	private final Boolean isBookmarked;
	private final List<BidHistoryResponse> bidHistory;

	public static AuctionDetailResponse from(
		final AuctionDetailDto dto,
		final Boolean isBookmarked,
		final List<BidHistoryResponse> bidHistory,
		final List<String> imageUrls
	) {
		long remainingSeconds = ChronoUnit.SECONDS.between(
			LocalDateTime.now(),
			dto.getEndAt()
		);

		return AuctionDetailResponse.builder()
			.auctionId(dto.getAuctionId())
			.productId(dto.getProductId())
			.sellerId(dto.getSellerId())
			.sellerNickname(dto.getSellerNickname())
			.name(dto.getName())
			.description(dto.getDescription())
			.category(dto.getCategory().name())
			.subCategory(dto.getSubCategory() != null ? dto.getSubCategory().name() : null)
			.status(dto.getStatus().name())
			.startPrice(dto.getStartPrice())
			.buyNowPrice(dto.getBuyNowPrice())
			.minBidStep(dto.getMinBidStep())
			.startAt(dto.getStartAt())
			.endAt(dto.getEndAt())
			.createdAt(dto.getCreatedAt())
			.currentHighestBid(dto.getCurrentHighestBid() != null
				? dto.getCurrentHighestBid()
				: dto.getStartPrice())
			.totalBidCount(dto.getTotalBidCount() != null ? dto.getTotalBidCount() : 0)
			.remainingTimeSeconds(Math.max(0, remainingSeconds))
			.imageUrls(imageUrls)
			.isBookmarked(isBookmarked)
			.bidHistory(bidHistory)
			.build();
	}
}
