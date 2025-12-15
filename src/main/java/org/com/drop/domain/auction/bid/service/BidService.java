package org.com.drop.domain.auction.bid.service;

import java.time.LocalDateTime;

import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.auction.repository.AuctionRepository;
import org.com.drop.domain.auction.bid.dto.request.BidRequestDto;
import org.com.drop.domain.auction.bid.dto.response.BidResponseDto;
import org.com.drop.domain.auction.bid.entity.Bid;
import org.com.drop.domain.auction.bid.repository.BidRepository;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.com.drop.global.exception.ErrorCode;
import org.com.drop.global.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BidService {

	private final BidRepository bidRepository;
	private final UserRepository userRepository;
	private final AuctionRepository auctionRepository;

	@Transactional
	public BidResponseDto placeBid(Long auctionId, Long userId, BidRequestDto requestDto) {

		User bidder = userRepository.findById(userId)
			.orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

		Auction auction = auctionRepository.findById(auctionId)
			.orElseThrow(() -> new ServiceException(ErrorCode.AUCTION_NOT_FOUND));

		Long bidAmount = requestDto.bidAmount();
		LocalDateTime now = LocalDateTime.now();

		if (auction.getStatus() != Auction.AuctionStatus.LIVE) {
			throw new ServiceException(ErrorCode.AUCTION_NOT_LIVE);
		}

		if (auction.getEndAt().isBefore(now)) {
			throw new ServiceException(ErrorCode.AUCTION_ALREADY_ENDED);
		}

		if (auction.getProduct().getSeller().getId().equals(bidder.getId())) {
			throw new ServiceException(ErrorCode.AUCTION_BIDDER_CANNOT_BE_OWNER);
		}

		Long highest = bidRepository.findTopByAuction_IdOrderByBidAmountDesc(auctionId)
			.map(Bid::getBidAmount)
			.orElse(Long.valueOf(auction.getStartPrice()));

		long minRequired = highest + auction.getMinBidStep();

		if (bidAmount < minRequired) {
			throw new ServiceException(ErrorCode.AUCTION_BID_AMOUNT_TOO_LOW);
		}

		Bid bid = Bid.builder()
			.auction(auction)
			.bidder(bidder)
			.bidAmount(bidAmount)
			.createdAt(now)
			.build();

		bidRepository.save(bid);

		boolean isHighestBidder = true;
		Long currentHighestBid = bidAmount;

		return BidResponseDto.of(
			auction.getId(),
			isHighestBidder,
			currentHighestBid,
			bid.getCreatedAt()
		);
	}
}
