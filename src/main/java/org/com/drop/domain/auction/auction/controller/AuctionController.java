package org.com.drop.domain.auction.auction.controller;

import org.com.drop.domain.auction.auction.dto.AuctionCreateRequest;
import org.com.drop.domain.auction.auction.dto.AuctionCreateResponse;
import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.auction.service.AuctionService;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.com.drop.global.rsData.RsData;
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

	//임시 설정
	private final UserRepository userRepository;

	@PostMapping
	public RsData<AuctionCreateResponse> addProduct(
		@RequestBody
		@Valid
		AuctionCreateRequest request) {
		//TODO : rq 구현 후 수정
		User actor = userRepository.findById(1L).get();
		Auction auction = auctionService.addAuction(request, actor);
		return new RsData<>(
			"SUCCESS",
			"200",
			"요청을 성공적으로 처리했습니다.",
			new AuctionCreateResponse(auction)
		);
	}
}
