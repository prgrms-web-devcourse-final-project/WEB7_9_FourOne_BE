package org.com.drop.domain.auction.bid;

import org.com.drop.domain.auction.bid.service.SseService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sse")
public class SseController {

	private final SseService sseService;

	@GetMapping(value = "/auctions/{auctionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public void subscribeAuction(@PathVariable Long auctionId) {
		sseService.subscribe(auctionId);
	}

}
