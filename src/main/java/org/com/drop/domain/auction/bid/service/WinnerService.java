package org.com.drop.domain.auction.bid.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.auction.repository.AuctionRepository;
import org.com.drop.domain.auction.bid.entity.Bid;
import org.com.drop.domain.auction.bid.entity.Winner;
import org.com.drop.domain.auction.bid.repository.BidRepository;
import org.com.drop.domain.auction.bid.repository.WinnerRepository;
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
			auction.end(now);
			return;
		}

		Bid topBid = topBidOpt.get();

		if (winnerRepository.existsByAuction_Id(auctionId)) {
			return;
		}

		Winner winner = Winner.builder()
			.auction(auction)
			.user(topBid.getBidder())
			.finalPrice(topBid.getBidAmount().intValue())
			.winTime(now)
			.build();

		winnerRepository.save(winner);

		auction.end(now);

		//결제도메인이벤트
	}
}
