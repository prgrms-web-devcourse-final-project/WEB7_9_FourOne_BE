package org.com.drop.domain.auction.auction.repository;

import org.com.drop.domain.auction.auction.entity.Auction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
}
