package org.com.drop.domain.auction.list.dto.response;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.com.drop.domain.auction.bid.dto.response.BidHistoryResponse;
import org.com.drop.domain.auction.list.repository.AuctionListRepositoryCustom.AuctionDetailDto;

/**
 * 경매 상세 응답 DTO
 */
public record AuctionDetailResponse(
	Long auctionId,
	Long productId,
	Long sellerId,
	String sellerNickname,
	String name,
	String description,
	String category,
	String subCategory,
	String status,
	Integer startPrice,
	Integer buyNowPrice,
	Integer minBidStep,
	LocalDateTime startAt,
	LocalDateTime endAt,
	LocalDateTime createdAt,
	Integer currentHighestBid,
	Integer totalBidCount,
	Long remainingTimeSeconds,
	List<String> imageUrls,
	Boolean isBookmarked,
	List<BidHistoryResponse> bidHistory
) {

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

		return new AuctionDetailResponse(
			dto.getAuctionId(),
			dto.getProductId(),
			dto.getSellerId(),
			dto.getSellerNickname(),
			dto.getName(),
			dto.getDescription(),
			dto.getCategory().name(),
			dto.getSubCategory() != null ? dto.getSubCategory().name() : null,
			dto.getStatus().name(),
			dto.getStartPrice(),
			dto.getBuyNowPrice(),
			dto.getMinBidStep(),
			dto.getStartAt(),
			dto.getEndAt(),
			dto.getCreatedAt(),
			dto.getCurrentHighestBid() != null
				? dto.getCurrentHighestBid()
				: dto.getStartPrice(),
			dto.getTotalBidCount() != null ? dto.getTotalBidCount() : 0,
			Math.max(0, remainingSeconds),
			imageUrls,
			isBookmarked,
			bidHistory
		);
	}
}
