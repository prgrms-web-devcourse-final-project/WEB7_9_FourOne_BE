package org.com.drop.domain.auction.list.service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.com.drop.domain.auction.bid.dto.response.BidHistoryResponse;
import org.com.drop.domain.auction.bid.entity.Bid;
import org.com.drop.domain.auction.bid.repository.BidRepository;
import org.com.drop.domain.auction.list.dto.request.AuctionSearchRequest;
import org.com.drop.domain.auction.list.dto.response.AuctionBidUpdate;
import org.com.drop.domain.auction.list.dto.response.AuctionCursorResponse;
import org.com.drop.domain.auction.list.dto.response.AuctionDetailResponse;
import org.com.drop.domain.auction.list.dto.response.AuctionHomeResponse;
import org.com.drop.domain.auction.list.dto.response.AuctionItemResponse;
import org.com.drop.domain.auction.list.repository.AuctionListRepository;
import org.com.drop.domain.auction.list.repository.AuctionListRepositoryCustom;
import org.com.drop.domain.user.entity.User;
import org.com.drop.global.aws.AmazonS3Client;
import org.com.drop.global.exception.ErrorCode;
import org.com.drop.global.exception.ServiceException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 경매 목록 서비스 (Redis 적용 및 성능 최적화)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionListService {

	private static final int BID_HISTORY_LIMIT = 10;
	private static final int HOME_LIMIT = 10;
	private static final String DEFAULT_IMAGE_URL =
		"https://drop-auction-bucket.s3.amazonaws.com/default/auction-default.jpg";

	private final AuctionListRepository auctionListRepository;
	private final BidRepository bidRepository;
	private final AmazonS3Client amazonS3Client;
	private final BookmarkCacheService bookmarkCacheService;

	/**
	 * 경매 목록 조회 (커서 기반 무한 스크롤)
	 */
	public AuctionCursorResponse getAuctions(
		final AuctionSearchRequest request,
		final User user
	) {
		log.debug("경매 목록 조회 - 정렬: {}, 카테고리: {}, 키워드: {}",
			request.sortType(), request.category(), request.keyword());

		// 1. DB 목록 조회
		List<AuctionListRepositoryCustom.AuctionItemDto> dtos =
			auctionListRepository.searchAuctions(request);

		boolean hasNext = dtos.size() > request.size();
		List<AuctionListRepositoryCustom.AuctionItemDto> resultDtos = hasNext
			? dtos.subList(0, request.size())
			: dtos;

		// 2. 사용자의 찜 목록 로드 (Redis 우선, DB 폴백)
		Set<Long> bookmarkedProductIds = loadUserBookmarks(user);

		// 3. DTO 변환 및 찜 여부 매핑
		String nextCursor = auctionListRepository.getNextCursor(
			dtos, request.size(), request.sortType());
		List<AuctionItemResponse> items = resultDtos.stream()
			.map(dto -> AuctionItemResponse.from(
				dto,
				bookmarkedProductIds.contains(dto.getProductId()),
				getImageUrlWithPresignedUrl(dto.getImageUrl())
			))
			.collect(Collectors.toList());

		return AuctionCursorResponse.of(items, nextCursor, hasNext);
	}

	/**
	 * 경매 상세 조회
	 */
	public AuctionDetailResponse getAuctionDetail(
		final Long auctionId,
		final User user
	) {
		AuctionListRepositoryCustom.AuctionDetailDto dto =
			auctionListRepository.findAuctionDetailById(auctionId)
				.orElseThrow(() -> new ServiceException(
					ErrorCode.AUCTION_NOT_FOUND,
					"경매를 찾을 수 없습니다. auctionId: %d", auctionId
				));

		// PageRequest를 사용하여 DB에서 최신순으로 10개만 조회
		List<Bid> bids = bidRepository.findAllByAuctionId(
			auctionId,
			PageRequest.of(0, BID_HISTORY_LIMIT, Sort.by(Sort.Direction.DESC, "createdAt"))
		).getContent();

		List<BidHistoryResponse> bidHistory = bids.stream()
			.map(BidHistoryResponse::from)
			.collect(Collectors.toList());

		// 사용자 찜 목록 로드
		Set<Long> bookmarkedProductIds = loadUserBookmarks(user);
		Boolean isBookmarked = bookmarkedProductIds.contains(dto.getProductId());

		// 이미지 URL 변환
		List<String> imageUrls = dto.getImageUrls().stream()
			.map(this::getImageUrlWithPresignedUrl)
			.collect(Collectors.toList());

		return AuctionDetailResponse.from(dto, isBookmarked, bidHistory, imageUrls);
	}

	/**
	 * 현재 최고 입찰가 조회
	 */
	public AuctionBidUpdate getCurrentHighestBid(final Long auctionId) {
		AuctionListRepositoryCustom.CurrentHighestBidDto highestBidDto =
			auctionListRepository.findCurrentHighestBid(auctionId)
				.orElseGet(() -> {
					Integer startPrice = auctionListRepository.findAuctionStartPrice(auctionId)
						.orElseThrow(() -> new ServiceException(
							ErrorCode.AUCTION_NOT_FOUND,
							"경매를 찾을 수 없습니다. auctionId: %d", auctionId
						));

					return AuctionListRepositoryCustom.CurrentHighestBidDto.builder()
						.currentHighestBid(startPrice)
						.bidderNickname(null)
						.bidTime(null)
						.build();
				});

		String maskedNickname = maskNickname(highestBidDto.getBidderNickname());

		return new AuctionBidUpdate(
			highestBidDto.getCurrentHighestBid(),
			maskedNickname
		);
	}

	/**
	 * 입찰 내역 조회
	 */
	public List<BidHistoryResponse> getBidHistory(Long auctionId, int size) {
		// PageRequest를 사용하여 필요한 개수만큼 최신순 조회
		List<Bid> bids = bidRepository.findAllByAuctionId(
			auctionId,
			PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt"))
		).getContent();

		return bids.stream()
			.map(BidHistoryResponse::from)
			.collect(Collectors.toList());
	}

	/**
	 * 홈화면 조회 (Redis 캐싱 적용)
	 */
	@Cacheable(value = "homeAuctions", key = "#user?.id ?: 'anonymous'")
	public AuctionHomeResponse getHomeAuctions(final User user) {
		// 사용자 찜 목록 로드
		Set<Long> bookmarkedProductIds = loadUserBookmarks(user);

		// 마감 임박 경매 조회 및 매핑
		List<AuctionItemResponse> endingSoon =
			auctionListRepository.findEndingSoonAuctions(HOME_LIMIT).stream()
				.map(dto -> AuctionItemResponse.from(
					dto,
					bookmarkedProductIds.contains(dto.getProductId()),
					getImageUrlWithPresignedUrl(dto.getImageUrl())
				))
				.collect(Collectors.toList());

		// 인기 경매 조회 및 매핑
		List<AuctionItemResponse> popular =
			auctionListRepository.findPopularAuctions(HOME_LIMIT).stream()
				.map(dto -> AuctionItemResponse.from(
					dto,
					bookmarkedProductIds.contains(dto.getProductId()),
					getImageUrlWithPresignedUrl(dto.getImageUrl())
				))
				.collect(Collectors.toList());

		return new AuctionHomeResponse(endingSoon, popular);
	}

	/**
	 * 사용자의 찜한 상품 ID 목록 로드 (Cache-Aside Pattern)
	 */
	private Set<Long> loadUserBookmarks(User user) {
		if (user == null) {
			return Collections.emptySet();
		}

		// 1. Redis 조회
		Set<Long> cachedBookmarks = bookmarkCacheService.getBookmarkedProductIds(user.getId());
		if (cachedBookmarks != null) {
			return cachedBookmarks;
		}

		// 2. DB 조회 (배치 조회)
		List<Long> dbBookmarks =
			auctionListRepository.findBookmarkedProductIdsByUserId(user.getId());

		// 3. Redis 적재
		bookmarkCacheService.cacheUserBookmarks(user.getId(), dbBookmarks);

		return Set.copyOf(dbBookmarks);
	}

	/**
	 * 이미지 URL을 Presigned URL로 변환
	 */
	private String getImageUrlWithPresignedUrl(String imageKey) {
		if (imageKey == null || imageKey.isBlank() || imageKey.equals(DEFAULT_IMAGE_URL)) {
			return DEFAULT_IMAGE_URL;
		}

		try {
			if (!imageKey.startsWith("http")) {
				return amazonS3Client.getPresignedUrl(imageKey);
			}
			return imageKey;
		} catch (Exception e) {
			log.warn("Presigned URL 생성 실패, 기본 이미지로 대체: {}", imageKey, e);
			return DEFAULT_IMAGE_URL;
		}
	}

	/**
	 * 닉네임 마스킹 처리
	 */
	private String maskNickname(String nickname) {
		if (nickname == null || nickname.length() <= 2) {
			return nickname;
		}
		return nickname.substring(0, 2) + "***";
	}
}
