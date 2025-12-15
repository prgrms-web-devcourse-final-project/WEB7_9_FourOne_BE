package org.com.drop.domain.auction.bid.controller;

import org.com.drop.domain.auction.bid.dto.response.BuyNowResponseDto;
import org.com.drop.domain.auction.bid.service.BuyNowService;
import org.com.drop.global.rsdata.RsData;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auctions")
public class BuyNowController {

	private final BuyNowService buyNowService;

	@PostMapping("/{auctionId}/buy-now")
	public RsData<BuyNowResponseDto> buyNow(
		@PathVariable Long auctionId,
		@RequestParam Long userId
	) {
		BuyNowResponseDto data = buyNowService.buyNow(auctionId, userId);
		return new RsData<>(data);
	}

}
