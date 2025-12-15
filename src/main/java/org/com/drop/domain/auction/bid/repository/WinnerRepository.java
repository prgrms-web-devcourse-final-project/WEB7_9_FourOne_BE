package org.com.drop.domain.auction.bid.repository;

import java.util.List;
import java.util.Optional;

import org.com.drop.domain.auction.bid.entity.Winner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WinnerRepository extends JpaRepository<Winner, Long> {

	boolean existsByAuction_Id(Long auctionId);

	@Query("SELECT w FROM Winner w JOIN FETCH w.user WHERE w.auction.id = :auctionId")
	Optional<Winner> findByAuction_Id(Long auctionId);


	List<Winner> findAllByAuction_Id(Long auctionId);

}
