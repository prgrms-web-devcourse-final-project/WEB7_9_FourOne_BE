package org.com.drop.domain.auction.auction.controller;

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
import org.com.drop.domain.user.entity.User;
import org.com.drop.global.rsdata.RsData;
import org.com.drop.global.security.auth.LoginUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auctions")
public class AuctionController {

	private final AuctionService auctionService;

	private final BuyNowService buyNowService;

	private final BidService bidService;

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

	@PostMapping("/{auctionId}/buy-now")
	public RsData<BuyNowResponseDto> buyNow(
		@PathVariable Long auctionId,
		@LoginUser User user,
		@RequestBody @Valid BuyNowRequestDto requestDto
	) {
		BuyNowResponseDto dto = buyNowService.buyNow(auctionId, user.getId(), requestDto);
		return new RsData<>(dto);
	}

	@PostMapping("/{auctionId}/bids")
	public RsData<BidResponseDto> placeBid(
		@PathVariable Long auctionId,
		@LoginUser User user,
		@RequestBody @Valid BidRequestDto requestDto
	) {
		BidResponseDto dto = bidService.placeBid(auctionId, user.getId(), requestDto);
		return new RsData<>(dto);
	}

	@GetMapping("/{auctionId}")
	public RsData<Page<BidHistoryResponse>> getBidHistory(
		@PathVariable Long auctionId,
		@PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable page) {
		Page<BidHistoryResponse> dto = bidService.getBidHistory(auctionId, page);
		return new RsData<>(dto);
	}

}
