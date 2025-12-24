package org.com.drop.domain.auction.list.controller;

import java.util.List;

import org.com.drop.domain.auction.bid.dto.response.BidHistoryResponse;
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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 경매 조회 전용 컨트롤러 (Query API)
 * - 조회 전용 (목록 / 상세 / 홈 / 입찰내역 / SSE)
 * - Command API(/api/v1/auctions)와 분리
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auction-list")
public class AuctionListController {

	private final AuctionListService auctionListService;
	private final SseService sseService;

	/**
	 * 경매 목록 조회
	 * GET /api/v1/auction-list
	 * ?sort={sort}&category={category}&keyword={keyword}&cursor={cursor}
	 */
	@GetMapping
	public RsData<AuctionCursorResponse> getAuctions(
		@Valid final AuctionSearchRequest request,
		@LoginUser final User user
	) {
		log.debug(
			"경매 목록 조회: sort={}, category={}, keyword={}, cursor={}",
			request.getSortType(),
			request.getCategory(),
			request.getKeyword(),
			request.getCursor()
		);

		AuctionCursorResponse response = auctionListService.getAuctions(request, user);
		return new RsData<>(response);
	}

	/**
	 * 경매 상세 조회
	 * GET /api/v1/auction-list/{auctionId}
	 */
	@GetMapping("/{auctionId}")
	public RsData<AuctionDetailResponse> getAuctionDetail(
		@PathVariable final Long auctionId,
		@LoginUser final User user
	) {
		log.debug("경매 상세 조회: auctionId={}", auctionId);

		AuctionDetailResponse response =
			auctionListService.getAuctionDetail(auctionId, user);
		return new RsData<>(response);
	}

	/**
	 * 현재 최고 입찰가 조회
	 * GET /api/v1/auction-list/{auctionId}/highest-bid
	 */
	@GetMapping("/{auctionId}/highest-bid")
	public RsData<AuctionBidUpdate> getHighestBid(
		@PathVariable final Long auctionId
	) {
		log.debug("현재 최고 입찰가 조회: auctionId={}", auctionId);

		AuctionBidUpdate response =
			auctionListService.getCurrentHighestBid(auctionId);
		return new RsData<>(response);
	}

	/**
	 * 홈 화면 경매 조회
	 * GET /api/v1/auction-list/home
	 */
	@GetMapping("/home")
	public RsData<AuctionHomeResponse> getHomeAuctions(
		@LoginUser final User user
	) {
		log.debug("홈 화면 경매 조회");

		AuctionHomeResponse response =
			auctionListService.getHomeAuctions(user);
		return new RsData<>(response);
	}

	/**
	 * 입찰 내역 조회 (뷰 전용)
	 * GET /api/v1/auction-list/{auctionId}/bids
	 */
	@GetMapping("/{auctionId}/bids")
	public RsData<List<BidHistoryResponse>> getBidHistory(
		@PathVariable final Long auctionId,
		@RequestParam(defaultValue = "10") final int size
	) {
		log.debug("입찰 내역 조회: auctionId={}, size={}", auctionId, size);

		List<BidHistoryResponse> response =
			auctionListService.getBidHistory(auctionId, size);
		return new RsData<>(response);
	}

	/**
	 * 실시간 최고 입찰가 SSE 스트림
	 * GET /api/v1/auction-list/{auctionId}/bid-stream
	 */
	@GetMapping(
		value = "/{auctionId}/bid-stream",
		produces = MediaType.TEXT_EVENT_STREAM_VALUE
	)
	public SseEmitter getBidStream(
		@PathVariable final Long auctionId
	) {
		log.debug("SSE 입찰 스트림 시작: auctionId={}", auctionId);

		return sseService.subscribe(auctionId);
	}
}
