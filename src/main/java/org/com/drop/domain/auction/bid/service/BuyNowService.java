package org.com.drop.domain.auction.bid.service;

import java.time.LocalDateTime;

import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.auction.repository.AuctionRepository;
import org.com.drop.domain.auction.bid.dto.response.BuyNowResponseDto;
import org.com.drop.domain.auction.bid.entity.Winner;
import org.com.drop.domain.auction.bid.repository.WinnerRepository;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.com.drop.global.exception.ErrorCode;
import org.com.drop.global.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BuyNowService {

	private final AuctionRepository auctionRepository;
	private final UserRepository userRepository;
	private final WinnerRepository winnerRepository;

	@Transactional
	public BuyNowResponseDto buyNow(Long auctionId, Long userId) {

		User buyer = userRepository.findById(userId)
			.orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));

		Auction auction = auctionRepository.findById(auctionId)
			.orElseThrow(() -> new ServiceException(ErrorCode.AUCTION_NOT_FOUND));

		LocalDateTime now = LocalDateTime.now();

		if (auction.getStatus() != Auction.AuctionStatus.LIVE) {
			throw new ServiceException(ErrorCode.AUCTION_NOT_LIVE);
		}

		if (auction.getEndAt().isBefore(now)) {
			throw new ServiceException(ErrorCode.AUCTION_ALREADY_ENDED);
		}

		if (auction.getBuyNowPrice() == null) {
			throw new ServiceException(ErrorCode.AUCTION_BUY_NOW_NOT_AVAILABLE);
		}

		if (auction.getProduct().getSeller().getId().equals(buyer.getId())) {
			throw new ServiceException(ErrorCode.AUCTION_BIDDER_CANNOT_BE_OWNER);
		}

		if (winnerRepository.existsByAuction_Id(auctionId)) {
			throw new ServiceException(ErrorCode.AUCTION_ALREADY_ENDED);
		}



		Winner winner = Winner.builder()
			.auction(auction)
			.user(buyer)
			.finalPrice(auction.getBuyNowPrice().intValue())
			.winTime(now)
			.build();

		Winner savedWinner = winnerRepository.save(winner);
		auction.end(now);

		//TODO 결제 도메인


		return new BuyNowResponseDto(
			auction.getId(),
			auction.getStatus().name(),
			savedWinner.getId(),
			savedWinner.getFinalPrice(),
			savedWinner.getWinTime()
			// payment.paymentRequestId(),
			// payment.paymentStatus()
		);
	}

}
