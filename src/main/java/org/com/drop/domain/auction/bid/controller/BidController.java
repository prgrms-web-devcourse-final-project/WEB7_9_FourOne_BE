package org.com.drop.domain.auction.bid.controller;

import org.com.drop.domain.auction.bid.dto.request.BidRequestDto;
import org.com.drop.domain.auction.bid.dto.response.BidResponseDto;
import org.com.drop.domain.auction.bid.service.BidService;
import org.com.drop.global.rsdata.RsData;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/bids")
@RequiredArgsConstructor
public class BidController {

	private final BidService bidService;
	// private final JwtUtil jwtUtil;

	@Operation(summary = "실시간 입찰", description = "JWT로 인증된 사용자가 실시간으로 경매에 입찰합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "입찰 성공",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = BidResponseDto.class)
			)
		),
		@ApiResponse(responseCode = "400", description = "잘못된 입력"),
		@ApiResponse(responseCode = "401", description = "인증 실패")
	})
	@PostMapping("/{auctionId}/bids")
	public RsData<BidResponseDto> placeBid(
		@PathVariable Long auctionId,
		@RequestParam Long userId,
		@RequestBody @Valid BidRequestDto requestDto
	) {
		BidResponseDto dto = bidService.placeBid(auctionId, userId, requestDto);
		return new RsData<>(dto);
	}


	// @PostMapping("/{auctionId}/bids")
	// public RsData<BidResponseDto> placeBid(
	// 	@RequestHeader("Authorization") String bearerToken,
	// 	@PathVariable Long auctionId, Long userId,
	// 	@Valid @RequestBody BidRequestDto request
	// ) {
	// 	BidResponseDto dto = bidService.placeBid(auctionId, userId, request);
	// 	return new RsData<>(dto);
	// }


}
