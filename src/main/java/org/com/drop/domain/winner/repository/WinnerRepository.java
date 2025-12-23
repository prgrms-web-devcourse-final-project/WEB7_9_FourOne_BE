package org.com.drop.domain.winner.repository;

import java.util.List;
import java.util.Optional;

import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.winner.domain.Winner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WinnerRepository extends JpaRepository<Winner, Long> {

	boolean existsByAuction_Id(Long auctionId);

	@Query("SELECT w FROM Winner w WHERE w.auction.id = :auctionId")
	Optional<Winner> findByAuction_Id(Long auctionId);

	List<Winner> findAllByAuction_Id(Long auctionId);

	List<Winner> findByAuctionIn(List<Auction> auctions);
}
