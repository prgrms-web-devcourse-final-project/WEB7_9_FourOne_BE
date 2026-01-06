package org.com.drop.domain.auction.bid.repository;

import java.util.List;
import java.util.Optional;

import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.bid.entity.Bid;
import org.com.drop.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BidRepository extends JpaRepository<Bid, Long> {
	Optional<Bid> findTopByAuction_IdOrderByBidAmountDesc(Long auctionId);

	@EntityGraph(attributePaths = {"bidder"})
	Page<Bid> findAllByAuctionId(Long auctionId, Pageable pageable);


	@Query("SELECT b.auction.id, COUNT(b) FROM Bid b WHERE b.auction IN :auctions GROUP BY b.auction.id")
	List<Object[]> countByAuctionIn(@Param("auctions") List<Auction> auctions);

	@Query(
		value = """
				SELECT b FROM Bid b
				JOIN FETCH b.auction a
				JOIN FETCH a.product p
				WHERE b.bidder = :user
				AND b.id IN (
					SELECT MAX(b2.id)
					FROM Bid b2
					WHERE b2.bidder = :user
					GROUP BY b2.auction.id
				)
				ORDER BY b.createdAt DESC
				""", countQuery = """
				SELECT COUNT(DISTINCT b.auction.id)
				FROM Bid b
				WHERE b.bidder = :user
				"""
	)
	Page<Bid> findMyLatestBidsPerAuction(@Param("user") User user, Pageable pageable);

	boolean existsByBidderAndAuctionStatus(User bidder, Auction.AuctionStatus status);
}
