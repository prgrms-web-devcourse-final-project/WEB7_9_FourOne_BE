package org.com.drop.domain.auction.auction.controller;

import org.com.drop.domain.auction.auction.dto.AuctionCreateRequest;
import org.com.drop.domain.auction.auction.dto.AuctionCreateResponse;
import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.auction.service.AuctionService;
import org.com.drop.domain.user.entity.User;
import org.com.drop.global.rsdata.RsData;
import org.com.drop.global.security.auth.LoginUser;
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
}
