package org.com.drop.domain.auction.bid.bidEvent;

import org.com.drop.domain.auction.bid.service.SseService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BidEventListener {

	private final SseService sseService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleBidSuccess(BidSuccessEvent event) {
		sseService.notifyHighestPrice(event.auctionId(), event.newhighestBid());
	}
}
