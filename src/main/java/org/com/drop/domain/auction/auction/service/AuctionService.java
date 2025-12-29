package org.com.drop.domain.auction.auction.service;

import java.time.LocalDateTime;

import org.com.drop.domain.auction.auction.dto.AuctionCreateRequest;
import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.auction.repository.AuctionRepository;
import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.auction.product.service.ProductService;
import org.com.drop.domain.user.entity.User;
import org.com.drop.global.exception.ErrorCode;
import org.com.drop.global.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuctionService {
	private final ProductService productService;
	private final AuctionRepository auctionRepository;
	public Auction addAuction(AuctionCreateRequest request, User actor) {
		Product product = productService.findProductById(request.product_id());
		productService.validUser(product.getSeller().getId(), actor);
		Auction auction = new Auction(
			product,
			request.startPrice(),
			request.buyNowPrice(),
			request.midBidStep(),
			request.startAt(),
			request.endAt(),
			Auction.AuctionStatus.SCHEDULED);
		return auctionRepository.save(auction);
	}

	@Transactional
	public void startAuction(Long auctionId) {
		Auction auction = auctionRepository.findById(auctionId)
			.orElseThrow(() -> new ServiceException(ErrorCode.AUCTION_NOT_FOUND, "요청하신 상품 ID를 찾을 수 없습니다." ));

		auction.start(LocalDateTime.now());
	}

	@Transactional
	public void endAuction(Long auctionId) {
		Auction auction = auctionRepository.findById(auctionId)
			.orElseThrow(() -> new ServiceException(ErrorCode.AUCTION_NOT_FOUND, "요청하신 상품 ID를 찾을 수 없습니다." ));

		auction.end(LocalDateTime.now());
	}
}
