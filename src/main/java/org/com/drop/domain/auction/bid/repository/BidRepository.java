package org.com.drop.domain.auction.bid.repository;

import java.util.Optional;

import org.com.drop.domain.auction.bid.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidRepository extends JpaRepository<Bid, Long> {
	Optional<Bid> findTopByAuction_IdOrderByBidAmountDesc(Long auctionId);
}
