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
			.orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND,"해당 사용자를 찾을 수 없습니다." ));

		Auction auction = auctionRepository.findById(auctionId)
			.orElseThrow(() -> new ServiceException(ErrorCode.AUCTION_NOT_FOUND, "요청하신 상품 ID를 찾을 수 없습니다."));

		LocalDateTime now = LocalDateTime.now();

		if (auction.getStatus() != Auction.AuctionStatus.LIVE) {
			throw new ServiceException(ErrorCode.AUCTION_NOT_LIVE,"진행 중인 경매가 아닙니다." );
		}

		if (auction.getEndAt().isBefore(now)) {
			throw new ServiceException(ErrorCode.AUCTION_ALREADY_ENDED, "이미 경매가 종료되었거나, 즉시 구매가 완료되었습니다.") ;
		}

		if (auction.getBuyNowPrice() == null) {
			throw new ServiceException(ErrorCode.AUCTION_BUY_NOW_NOT_AVAILABLE,"즉시 구매가 불가능한 상품입니다." );
		}

		if (auction.getProduct().getSeller().getId().equals(buyer.getId())) {
			throw new ServiceException(ErrorCode.AUCTION_BIDDER_CANNOT_BE_OWNER, "경매 상품의 판매자는 입찰할 수 없습니다.");
		}

		if (winnerRepository.existsByAuction_Id(auctionId)) {
			throw new ServiceException(ErrorCode.AUCTION_ALREADY_ENDED,"이미 경매가 종료되었거나, 즉시 구매가 완료되었습니다." );
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
