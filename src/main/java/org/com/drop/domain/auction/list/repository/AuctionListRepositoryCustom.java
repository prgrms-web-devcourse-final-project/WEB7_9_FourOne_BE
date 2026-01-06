package org.com.drop.domain.auction.list.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.list.dto.SortType;
import org.com.drop.domain.auction.list.dto.request.AuctionSearchRequest;
import org.com.drop.domain.auction.product.entity.Product;

/**
 * 경매 목록 QueryDSL 커스텀 레포지토리 인터페이스
 */
public interface AuctionListRepositoryCustom {

	// ==================== 조회 메서드 ====================

	List<AuctionItemDto> searchAuctions(AuctionSearchRequest request);

	Optional<AuctionDetailDto> findAuctionDetailById(Long auctionId);

	List<AuctionItemDto> findEndingSoonAuctions(int limit);

	List<AuctionItemDto> findPopularAuctions(int limit);

	List<BidHistoryDto> findBidHistory(Long auctionId, int limit);

	Optional<CurrentHighestBidDto> findCurrentHighestBid(Long auctionId);

	Optional<Integer> findAuctionStartPrice(Long auctionId);

	// ==================== 커서 및 페이징 ====================

	String getNextCursor(List<AuctionItemDto> results, int size, SortType sortType);

	// ==================== 찜 관련 메서드 ====================

	/**
	 * 특정 사용자가 찜한 모든 상품 ID 목록 조회 (배치 조회)
	 * 성능 최적화를 위한 배치 쿼리
	 *
	 * @param userId 사용자 ID
	 * @return 찜한 상품 ID 목록
	 */
	List<Long> findBookmarkedProductIdsByUserId(Long userId);

	// ==================== 내부 DTO 클래스들 ====================

	/**
	 * 경매 아이템 DTO
	 */
	@lombok.Getter
	@lombok.Builder
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
	@lombok.Getter
	@lombok.Builder
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
	@lombok.Getter
	@lombok.Builder
	class BidHistoryDto {
		private final Long bidId;
		private final String bidderNickname;
		private final Integer bidAmount;
		private final LocalDateTime createdAt;
	}

	/**
	 * 현재 최고 입찰가 DTO
	 */
	@lombok.Getter
	@lombok.Builder
	@lombok.AllArgsConstructor
	class CurrentHighestBidDto {
		private final Integer currentHighestBid;
		private final String bidderNickname;
		private final LocalDateTime bidTime;
	}
}
