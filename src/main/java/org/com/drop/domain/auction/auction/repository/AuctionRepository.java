package org.com.drop.domain.auction.auction.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.com.drop.domain.auction.auction.entity.Auction;
import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
	Optional<Auction> findByProductId(Long id);

	List<Auction> findByProductInAndDeletedAtIsNullOrderByIdDesc(List<Product> products);

	boolean existsByProductSellerAndStatus(User seller, Auction.AuctionStatus status);

	List<Auction> findAllByStatusAndStartAtBefore(Auction.AuctionStatus status, LocalDateTime now);

	List<Auction> findAllByStatusAndEndAtBefore(Auction.AuctionStatus status, LocalDateTime now);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT a FROM Auction a WHERE a.id = :id")
	Optional<Auction> findByIdWithPessimisticLock(@Param("id") Long id);
}
