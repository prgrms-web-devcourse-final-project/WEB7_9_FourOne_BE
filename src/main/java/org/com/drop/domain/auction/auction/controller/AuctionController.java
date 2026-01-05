package org.com.drop.domain.auction.auction.controller;

import java.util.List;

import org.com.drop.domain.auction.auction.dto.AuctionCreateRequest;
import org.com.drop.domain.auction.auction.dto.AuctionCreateResponse;
import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.auction.service.AuctionService;
import org.com.drop.domain.auction.bid.dto.request.BidRequestDto;
import org.com.drop.domain.auction.bid.dto.request.BuyNowRequestDto;
import org.com.drop.domain.auction.bid.dto.response.BidHistoryResponse;
import org.com.drop.domain.auction.bid.dto.response.BidResponseDto;
import org.com.drop.domain.auction.bid.dto.response.BuyNowResponseDto;
import org.com.drop.domain.auction.bid.service.BidService;
import org.com.drop.domain.auction.bid.service.BuyNowService;
import org.com.drop.domain.auction.bid.service.SseService;
import org.com.drop.domain.auction.list.dto.request.AuctionSearchRequest;
import org.com.drop.domain.auction.list.dto.response.AuctionBidUpdate;
import org.com.drop.domain.auction.list.dto.response.AuctionCursorResponse;
import org.com.drop.domain.auction.list.dto.response.AuctionDetailResponse;
import org.com.drop.domain.auction.list.dto.response.AuctionHomeResponse;
import org.com.drop.domain.auction.list.service.AuctionListService;
import org.com.drop.domain.user.entity.User;
import org.com.drop.global.rsdata.RsData;
import org.com.drop.global.security.auth.LoginUser;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 경매 통합 컨트롤러 (생성 / 입찰 / 조회)
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auctions")
public class AuctionController {

	private final AuctionService auctionService;
	private final BuyNowService buyNowService;
	private final BidService bidService;
	private final AuctionListService auctionListService;
	private final SseService sseService;

	// ==================== 경매 생성 ====================

	/**
	 * 경매 생성
	 * POST /api/v1/auctions
	 */
	@PostMapping
	public RsData<AuctionCreateResponse> addProduct(
		@LoginUser User actor,
		@RequestBody @Valid AuctionCreateRequest request
	) {
		Auction auction = auctionService.addAuction(request, actor);

		return new RsData<>(
			201,
			new AuctionCreateResponse(auction)
		);
	}

	// ==================== 경매 입찰 / 구매 ====================

	/**
	 * 즉시 구매
	 * POST /api/v1/auctions/{auctionId}/buy-now
	 */
	@PostMapping("/{auctionId}/buy-now")
	public RsData<BuyNowResponseDto> buyNow(
		@PathVariable Long auctionId,
		@LoginUser User user,
		@RequestBody @Valid BuyNowRequestDto requestDto
	) {
		BuyNowResponseDto response =
			buyNowService.buyNow(auctionId, user.getId(), requestDto);

		return new RsData<>(response);
	}

	/**
	 * 입찰하기
	 * POST /api/v1/auctions/{auctionId}/bids
	 */
	@PostMapping("/{auctionId}/bids")
	public RsData<BidResponseDto> placeBid(
		@PathVariable Long auctionId,
		@LoginUser User user,
		@RequestBody @Valid BidRequestDto requestDto
	) {
		BidResponseDto response =
			bidService.placeBid(auctionId, user.getId(), requestDto);

		return new RsData<>(response);
	}

	// ==================== 경매 조회 ====================

	/**
	 * 경매 목록 조회 (커서 기반 무한 스크롤)
	 * GET /api/v1/auctions
	 */
	@GetMapping
	public RsData<AuctionCursorResponse> getAuctions(
		@Valid @ParameterObject AuctionSearchRequest request,
		@LoginUser(required = false) User user
	) {
		log.debug(
			"경매 목록 조회 - sortType={}, category={}, keyword={}, cursor={}",
			request.getSortType(),
			request.getCategory(),
			request.getKeyword(),
			request.getCursor()
		);

		AuctionCursorResponse response =
			auctionListService.getAuctions(request, user);

		return new RsData<>(response);
	}

	/**
	 * 경매 상세 조회
	 * GET /api/v1/auctions/{auctionId}
	 */
	@GetMapping("/{auctionId}")
	public RsData<AuctionDetailResponse> getAuctionDetail(
		@PathVariable Long auctionId,
		@LoginUser(required = false) User user
	) {
		log.debug("경매 상세 조회 - auctionId={}", auctionId);

		AuctionDetailResponse response =
			auctionListService.getAuctionDetail(auctionId, user);

		return new RsData<>(response);
	}

	/**
	 * 현재 최고 입찰가 조회
	 * GET /api/v1/auctions/{auctionId}/highest-bid
	 */
	@GetMapping("/{auctionId}/highest-bid")
	public RsData<AuctionBidUpdate> getHighestBid(
		@PathVariable Long auctionId
	) {
		log.debug("현재 최고가 조회 - auctionId={}", auctionId);

		AuctionBidUpdate response =
			auctionListService.getCurrentHighestBid(auctionId);

		return new RsData<>(response);
	}

	/**
	 * 홈 화면 조회
	 * GET /api/v1/auctions/home
	 */
	@GetMapping("/home")
	public RsData<AuctionHomeResponse> getHomeAuctions(
		@AuthenticationPrincipal User user
	) {
		log.debug("홈 화면 조회");

		AuctionHomeResponse response =
			auctionListService.getHomeAuctions(user);

		return new RsData<>(response);
	}

	/**
	 * 입찰 내역 조회 (페이지네이션)
	 * GET /api/v1/auctions/{auctionId}/bids
	 */
	@GetMapping("/{auctionId}/bids")
	public RsData<Page<BidHistoryResponse>> getBidHistory(
		@PathVariable Long auctionId,
		@ParameterObject
		@PageableDefault(
			size = 10,
			sort = "createdAt",
			direction = Sort.Direction.DESC
		) Pageable pageable
	) {
		Page<BidHistoryResponse> response =
			bidService.getBidHistory(auctionId, pageable);

		return new RsData<>(response);
	}

	/**
	 * 입찰 내역 간단 조회 (커서 기반용)
	 * GET /api/v1/auctions/{auctionId}/bid-list
	 */
	@GetMapping("/{auctionId}/bid-list")
	public RsData<List<BidHistoryResponse>> getBidList(
		@PathVariable Long auctionId,
		@RequestParam(defaultValue = "10") int size
	) {
		log.debug(
			"입찰 내역 목록 조회 - auctionId={}, size={}",
			auctionId,
			size
		);

		List<BidHistoryResponse> response =
			auctionListService.getBidHistory(auctionId, size);

		return new RsData<>(response);
	}

	// ==================== 실시간 SSE ====================

	/**
	 * 실시간 최고가 SSE 스트림
	 * GET /api/v1/auctions/{auctionId}/bid-stream
	 */
	@GetMapping(
		value = "/{auctionId}/bid-stream",
		produces = MediaType.TEXT_EVENT_STREAM_VALUE
	)
	public SseEmitter getBidStream(
		@PathVariable Long auctionId
	) {
		log.debug("SSE 입찰 스트림 시작 - auctionId={}", auctionId);

		return sseService.subscribe(auctionId);
	}
}
