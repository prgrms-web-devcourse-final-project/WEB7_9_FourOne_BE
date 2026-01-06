package org.com.drop.domain.auction.bid.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.auction.repository.AuctionRepository;
import org.com.drop.domain.auction.bid.entity.Bid;
import org.com.drop.domain.auction.bid.repository.BidRepository;
import org.com.drop.domain.notification.service.NotificationService;
import org.com.drop.domain.winner.domain.Winner;
import org.com.drop.domain.winner.repository.WinnerRepository;
import org.com.drop.global.exception.ErrorCode;
import org.com.drop.global.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WinnerService {
	private final AuctionRepository auctionRepository;
	private final BidRepository bidRepository;
	private final WinnerRepository winnerRepository;
	private final NotificationService notificationService;

	@Transactional
	public void finalizeAuction(Long auctionId) {

		Auction auction = auctionRepository.findById(auctionId)
			.orElseThrow(() -> new ServiceException(ErrorCode.AUCTION_NOT_FOUND, "요청하신 상품 ID를 찾을 수 없습니다."));

		LocalDateTime now = LocalDateTime.now();

		if (auction.getStatus() == Auction.AuctionStatus.ENDED
			|| auction.getStatus() == Auction.AuctionStatus.CANCELLED) {
			return;
		}

		if (auction.getEndAt().isAfter(now)) {
			return;
		}

		Optional<Bid> topBidOpt = bidRepository.findTopByAuction_IdOrderByBidAmountDesc(auctionId);

		if (topBidOpt.isEmpty()) {
			auction.expire();
			notificationService.addNotification(auction.getProduct().getSeller(), "경매가 유찰되었습니다.");
			return;
		}


		if (winnerRepository.existsByAuction_Id(auctionId)) {
			return;
		}

		Bid topBid = topBidOpt.get();

		Winner winner = Winner.builder()
			.auction(auction)
			.sellerId(auction.getProduct().getSeller().getId())
			.userId(topBid.getBidder().getId())
			.finalPrice(topBid.getBidAmount())
			.winTime(now)
			.build();

		winnerRepository.save(winner);
		auction.expire();

		notificationService.addNotification(topBid.getBidder(), "경매가 낙찰되었습니다.");
		notificationService.addNotification(auction.getProduct().getSeller(), "경매가 낙찰되었습니다.");

	}
}
