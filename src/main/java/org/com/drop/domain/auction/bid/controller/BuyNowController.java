package org.com.drop.domain.auction.bid.controller;

import org.com.drop.domain.auction.bid.dto.response.BuyNowResponseDto;
import org.com.drop.domain.auction.bid.service.BuyNowService;
import org.com.drop.domain.user.entity.User;
import org.com.drop.global.rsdata.RsData;
import org.com.drop.global.security.auth.LoginUser;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
		@LoginUser User user
	) {
		BuyNowResponseDto dto = buyNowService.buyNow(auctionId, user.getId());
		return new RsData<>(dto);
	}

}
