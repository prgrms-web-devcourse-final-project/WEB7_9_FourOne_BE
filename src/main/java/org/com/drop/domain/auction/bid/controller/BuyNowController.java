package org.com.drop.domain.auction.bid.controller;

import org.com.drop.domain.auction.bid.dto.response.BuyNowResponseDto;
import org.com.drop.domain.auction.bid.service.BuyNowService;
import org.com.drop.global.rsdata.RsData;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auctions")
public class BuyNowController {

	private final BuyNowService buyNowService;

	@Operation(summary = "즉시 구매", description = "JWT로 인증된 사용자가 경매를 즉시 구매합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "즉시 구매 성공",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = BuyNowResponseDto.class)
			)
		),
		@ApiResponse(responseCode = "400", description = "잘못된 입력"),
		@ApiResponse(responseCode = "401", description = "인증 실패")
	})
	@PostMapping("/{auctionId}/buy-now")
	public RsData<BuyNowResponseDto> buyNow(
		@PathVariable Long auctionId,
		@RequestParam Long userId
	) {
		BuyNowResponseDto data = buyNowService.buyNow(auctionId, userId);
		return new RsData<>(data);
	}

}
