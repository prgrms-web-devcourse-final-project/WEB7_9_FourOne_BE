package org.com.drop.domain.auction.list.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.list.dto.request.AuctionSearchRequest;
import org.com.drop.domain.auction.product.entity.Product;

import lombok.Builder;
import lombok.Getter;

/**
 * 경매 목록 QueryDSL 커스텀 레포지토리 인터페이스
 */
public interface AuctionListRepositoryCustom {

	List<AuctionItemDto> searchAuctions(AuctionSearchRequest request);

	String getNextCursor(List<AuctionItemDto> results, int size);

	Optional<AuctionDetailDto> findAuctionDetailById(Long auctionId);

	List<AuctionItemDto> findEndingSoonAuctions(int limit);

	List<AuctionItemDto> findPopularAuctions(int limit);

	List<BidHistoryDto> findBidHistory(Long auctionId, int limit);

	boolean isBookmarked(Long productId, Long userId);

	/**
	 * 현재 최고 입찰가 조회
	 */
	Optional<CurrentHighestBidDto> findCurrentHighestBid(Long auctionId);

	/**
	 * 경매 시작가 조회
	 */
	Optional<Integer> findAuctionStartPrice(Long auctionId);

	/**
	 * 경매 아이템 DTO
	 */
	@Getter
	@Builder
	@lombok.AllArgsConstructor
	class AuctionItemDto {
		private final Long auctionId;
		private final Long productId;
		private final String name;
		private final String imageUrl;
		private final Auction.AuctionStatus status;
		private final Product.Category category;
		private final Product.SubCategory subCategory;
		private final Integer startPrice;
		private final Integer currentHighestBid;
		private final LocalDateTime endAt;
		private final Integer bookmarkCount;
		private final Integer bidCount;
		private final LocalDateTime createdAt;
		private final Integer score;
	}

	/**
	 * 경매 상세 DTO
	 */
	@Getter
	@Builder
	@lombok.AllArgsConstructor
	class AuctionDetailDto {
		private final Long auctionId;
		private final Long productId;
		private final Long sellerId;
		private final String sellerNickname;
		private final String name;
		private final String description;
		private final Product.Category category;
		private final Product.SubCategory subCategory;
		private final Auction.AuctionStatus status;
		private final Integer startPrice;
		private final Integer buyNowPrice;
		private final Integer minBidStep;
		private final LocalDateTime startAt;
		private final LocalDateTime endAt;
		private final LocalDateTime createdAt;
		private final Integer currentHighestBid;
		private final Integer totalBidCount;
		private final List<String> imageUrls;
	}

	/**
	 * 입찰 내역 DTO
	 */
	@Getter
	@Builder
	class BidHistoryDto {
		private final Long bidId;
		private final String bidderNickname;
		private final Integer bidAmount;
		private final LocalDateTime createdAt;
	}

	/**
	 * 현재 최고 입찰가 DTO
	 */
	@Getter
	@Builder
	@lombok.AllArgsConstructor
	class CurrentHighestBidDto {
		private final Integer currentHighestBid;
		private final String bidderNickname;
		private final LocalDateTime bidTime;
	}
}
