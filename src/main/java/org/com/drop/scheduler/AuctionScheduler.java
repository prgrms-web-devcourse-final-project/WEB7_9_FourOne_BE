package org.com.drop.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.auction.repository.AuctionRepository;
import org.com.drop.domain.auction.auction.service.AuctionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionScheduler {

	private final AuctionRepository auctionRepository;
	private final AuctionService auctionService;

	@Scheduled(cron = "0 0/1 * * * *")
	public void runAuctionScheduler() {
		LocalDateTime now = LocalDateTime.now();

		List<Auction> scheduledAuctions = auctionRepository.findAllByStatusAndStartAtBefore(
			Auction.AuctionStatus.SCHEDULED, now
		);

		for (Auction auction : scheduledAuctions) {
			auctionService.startAuction(auction.getId());
		}

		List<Auction> liveAuctions = auctionRepository.findAllByStatusAndEndAtBefore(
			Auction.AuctionStatus.LIVE, now
		);

		for (Auction auction : liveAuctions) {
			auctionService.endAuction(auction.getId());
		}
	}

}
