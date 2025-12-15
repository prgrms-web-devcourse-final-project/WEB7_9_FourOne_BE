package org.com.drop.domain.auction.bid.repository;

import java.util.Optional;

import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.bid.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BidRepository extends JpaRepository<Bid, Long> {
	Optional<Bid> findTopByAuction_IdOrderByBidAmountDesc(Long auctionId);

	@Query("""
        select b 
        from Bid b
        where b.auction = :auction
        order by b.bidAmount desc, b.createdAt asc
        """)
	Optional<Bid> findTopByAuctionOrderByAmountDescAndCreatedAtAsc(@Param("auction") Auction auction);

}
