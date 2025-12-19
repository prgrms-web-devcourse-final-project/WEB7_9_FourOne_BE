package org.com.drop.domain.auction.bid.controller;

import org.com.drop.domain.auction.bid.dto.request.BidRequestDto;
import org.com.drop.domain.auction.bid.dto.response.BidResponseDto;
import org.com.drop.domain.auction.bid.service.BidService;
import org.com.drop.domain.user.entity.User;
import org.com.drop.global.rsdata.RsData;
import org.com.drop.global.security.auth.LoginUser;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/bids")
@RequiredArgsConstructor
public class BidController {

	private final BidService bidService;
	// private final JwtUtil jwtUtil;


	@PostMapping("/{auctionId}/bids")
	public RsData<BidResponseDto> placeBid(
		@PathVariable Long auctionId,
		@LoginUser User user,
		@RequestBody @Valid BidRequestDto requestDto
	) {
		BidResponseDto dto = bidService.placeBid(auctionId, user.getId(), requestDto);
		return new RsData<>(dto);
	}
}
