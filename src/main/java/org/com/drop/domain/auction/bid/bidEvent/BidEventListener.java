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

	// 트랜잭션 커밋(DB 저장)이 성공한 후에만 실행됨
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleBidSuccess(BidSuccessEvent event) {
		sseService.notifyHighestPrice(event.auctionId(), event.newhighestBid());
	}
}
