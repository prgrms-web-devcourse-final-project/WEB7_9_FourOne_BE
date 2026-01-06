package org.com.drop.domain.auction.list.service;

import java.util.List;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 경매 목록 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionListService {

	private static final int BID_HISTORY_LIMIT = 10;
	private static final int HOME_LIMIT = 10;
	private static final String DEFAULT_IMAGE_URL = "https://drop-auction-bucket.s3.amazonaws.com/default/auction-default.jpg";

	private final AuctionListRepository auctionListRepository;
	private final BidRepository bidRepository;
	private final AmazonS3Client amazonS3Client;

	/**
	 * 경매 목록 조회 (커서 기반 무한 스크롤)
	 */
	public AuctionCursorResponse getAuctions(
		final AuctionSearchRequest request,
		final User user
	) {
		log.debug("경매 목록 조회 - 정렬: {}, 카테고리: {}, 키워드: {}, 커서: {}",
			request.getSortType(), request.getCategory(), request.getKeyword(), request.getCursor());

		List<AuctionListRepositoryCustom.AuctionItemDto> dtos =
			auctionListRepository.searchAuctions(request);

		boolean hasNext = dtos.size() > request.getSize();
		List<AuctionListRepositoryCustom.AuctionItemDto> resultDtos = hasNext
			? dtos.subList(0, request.getSize())
			: dtos;

		// SortType을 전달하여 다음 커서 생성
		String nextCursor = auctionListRepository.getNextCursor(dtos, request.getSize(), request.getSortType());

		List<AuctionItemResponse> items = resultDtos.stream()
			.map(dto -> AuctionItemResponse.from(
				dto,
				getIsBookmarked(dto.getProductId(), user),
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

		// BidRepository에서 입찰 내역 조회
		List<Bid> bids = bidRepository.findAllByAuctionId(
			auctionId,
			PageRequest.of(0, BID_HISTORY_LIMIT)
		).getContent();

		List<BidHistoryResponse> bidHistory = bids.stream()
			.map(BidHistoryResponse::from)
			.collect(Collectors.toList());

		Boolean isBookmarked = getIsBookmarked(dto.getProductId(), user);

		// 이미지 URL들을 Presigned URL로 변환
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
					// 최고 입찰이 없으면 경매 시작가 조회
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

		// 마스킹 처리
		String bidderNickname = highestBidDto.getBidderNickname();
		String maskedNickname = (bidderNickname != null) ? maskNickname(bidderNickname) : null;

		return AuctionBidUpdate.builder()
			.currentHighestBid(highestBidDto.getCurrentHighestBid())
			.bidderNickname(maskedNickname)
			.build();
	}

	/**
	 * 입찰 내역 조회
	 */
	public List<BidHistoryResponse> getBidHistory(Long auctionId, int size) {
		List<Bid> bids = bidRepository.findAllByAuctionId(
			auctionId,
			PageRequest.of(0, size)
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
		List<AuctionItemResponse> endingSoon =
			auctionListRepository.findEndingSoonAuctions(HOME_LIMIT).stream()
				.map(dto -> AuctionItemResponse.from(
					dto,
					getIsBookmarked(dto.getProductId(), user),
					getImageUrlWithPresignedUrl(dto.getImageUrl())
				))
				.collect(Collectors.toList());

		List<AuctionItemResponse> popular =
			auctionListRepository.findPopularAuctions(HOME_LIMIT).stream()
				.map(dto -> AuctionItemResponse.from(
					dto,
					getIsBookmarked(dto.getProductId(), user),
					getImageUrlWithPresignedUrl(dto.getImageUrl())
				))
				.collect(Collectors.toList());

		return AuctionHomeResponse.builder()
			.endingSoon(endingSoon)
			.popular(popular)
			.build();
	}

	/**
	 * 이미지 URL을 Presigned URL로 변환
	 */
	private String getImageUrlWithPresignedUrl(String imageKey) {
		if (imageKey == null || imageKey.isBlank() || imageKey.equals(DEFAULT_IMAGE_URL)) {
			return DEFAULT_IMAGE_URL;
		}

		try {
			// S3 객체 키인 경우 Presigned URL 생성
			if (!imageKey.startsWith("http")) {
				return amazonS3Client.getPresignedUrl(imageKey);
			}
			// 이미 URL인 경우 그대로 반환
			return imageKey;
		} catch (Exception e) {
			log.warn("Presigned URL 생성 실패, 기본 이미지로 대체: {}", imageKey, e);
			return DEFAULT_IMAGE_URL;
		}
	}

	/**
	 * 찜 여부 확인 (로그인하지 않은 경우 false)
	 */
	private Boolean getIsBookmarked(final Long productId, final User user) {
		return user != null
			? auctionListRepository.isBookmarked(productId, user.getId())
			: false;
	}

	/**
	 * 닉네임 마스킹 처리
	 */
	private String maskNickname(String nickname) {
		if (nickname == null || nickname.length() <= 2) {
			return nickname;
		}
		// 앞 2글자 + *** 형식으로 마스킹
		return nickname.substring(0, 2) + "***";
	}
}
