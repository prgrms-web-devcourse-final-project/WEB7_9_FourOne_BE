package org.com.drop.domain.auction.bid.service;

import java.time.LocalDateTime;

import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.auction.repository.AuctionRepository;
import org.com.drop.domain.auction.bid.bidEvent.BidSuccessEvent;
import org.com.drop.domain.auction.bid.dto.request.BidRequestDto;
import org.com.drop.domain.auction.bid.dto.response.BidHistoryResponse;
import org.com.drop.domain.auction.bid.dto.response.BidResponseDto;
import org.com.drop.domain.auction.bid.entity.Bid;
import org.com.drop.domain.auction.bid.repository.BidRepository;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.com.drop.global.exception.ErrorCode;
import org.com.drop.global.exception.ServiceException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BidService {

	private final BidRepository bidRepository;
	private final UserRepository userRepository;
	private final AuctionRepository auctionRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public BidResponseDto placeBid(Long auctionId, Long userId, BidRequestDto requestDto) {

		User bidder = userRepository.findById(userId)
			.orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND, "해당 사용자를 찾을 수 없습니다."));

		Auction auction = auctionRepository.findById(auctionId)
			.orElseThrow(() -> new ServiceException(ErrorCode.AUCTION_NOT_FOUND, "요청하신 상품 ID를 찾을 수 없습니다." ));

		Long bidAmount = requestDto.bidAmount();
		LocalDateTime now = LocalDateTime.now();

		if (auction.getStatus() != Auction.AuctionStatus.LIVE) {
			throw new ServiceException(ErrorCode.AUCTION_NOT_LIVE, "진행 중인 경매가 아닙니다." );
		}

		if (auction.getEndAt().isBefore(now)) {
			throw new ServiceException(ErrorCode.AUCTION_ALREADY_ENDED, "이미 경매가 종료되었거나, 즉시 구매가 완료되었습니다.");
		}

		if (auction.getProduct().getSeller().getId().equals(bidder.getId())) {
			throw new ServiceException(ErrorCode.AUCTION_BIDDER_CANNOT_BE_OWNER, "경매 상품의 판매자는 입찰할 수 없습니다.");
		}

		Long highest = bidRepository.findTopByAuction_IdOrderByBidAmountDesc(auctionId)
			.map(Bid::getBidAmount)
			.orElse(Long.valueOf(auction.getStartPrice()));

		long minRequired = highest + auction.getMinBidStep();

		if (bidAmount < minRequired) {
			throw new ServiceException(ErrorCode.AUCTION_BID_AMOUNT_TOO_LOW,
				"입찰 금액이 현재 최고가보다 낮거나 최소 입찰 단위를 충족하지 못했습니다." );
		}

		Bid bid = Bid.builder()
			.auction(auction)
			.bidder(bidder)
			.bidAmount(bidAmount)
			.createdAt(now)
			.build();

		bidRepository.save(bid);

		eventPublisher.publishEvent(new BidSuccessEvent(auctionId, bidAmount));

		boolean isHighestBidder = true;
		Long currentHighestBid = bidAmount;

		return BidResponseDto.of(
			auction.getId(),
			isHighestBidder,
			currentHighestBid,
			bid.getCreatedAt()
		);
	}

	public Page<BidHistoryResponse> getBidHistory(Long auctionId, Pageable pageable) {
		Page<Bid> bidsPage = bidRepository.findAllByAuctionId( auctionId, pageable);
		return bidsPage.map(BidHistoryResponse::from);

	}
}
