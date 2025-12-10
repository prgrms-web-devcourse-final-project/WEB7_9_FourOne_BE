package org.com.drop.domain.auction.auction.service;

import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.auction.repository.AuctionRepository;
import org.com.drop.domain.auction.product.dto.AuctionCreateRequest;
import org.com.drop.domain.auction.product.entity.Product;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuctionService {
	private  final AuctionRepository auctionRepository;

	public Auction addAuction(AuctionCreateRequest request, Product product) {
		Auction auction = new Auction(
			product,
			request.startPrice(),
			request.buyNowPrice(),
			request.minBidStep(),
			request.startAt(),
			request.endAt(),
			Auction.AuctionStatus.SCHEDULED
		);
		return auctionRepository.save(auction);
	}
}
