package org.com.drop.domain.auction.auction.repository;

import java.util.Optional;

import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
	Optional<Auction> findByProduct(Product product);
}
