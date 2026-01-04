package org.com.drop.domain.auction.list.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.com.drop.domain.auction.auction.entity.Auction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 경매 목록 JPA 레포지토리
 *
 * @author 드랍팀
 */
@Repository
public interface AuctionListRepository extends JpaRepository<Auction, Long>, AuctionListRepositoryCustom {

	/**
	 * ID로 삭제되지 않은 경매 조회
	 *
	 * @param id 경매 ID
	 * @return 경매 Optional
	 */
	@Query("SELECT a FROM Auction a WHERE a.id = :id AND a.deletedAt IS NULL")
	Optional<Auction> findByIdAndNotDeleted(@Param("id") Long id);

	/**
	 * 판매자 ID로 경매 목록 조회
	 *
	 * @param sellerId 판매자 ID
	 * @param pageable 페이지 정보
	 * @return 경매 페이지
	 */
	@Query("SELECT a FROM Auction a JOIN a.product p WHERE p.seller.id = :sellerId AND a.deletedAt IS NULL")
	Page<Auction> findBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);

	/**
	 * 상태별 경매 목록 조회
	 *
	 * @param status 경매 상태
	 * @param pageable 페이지 정보
	 * @return 경매 페이지
	 */
	Page<Auction> findByStatus(Auction.AuctionStatus status, Pageable pageable);

	/**
	 * 카테고리별 경매 목록 조회
	 *
	 * @param category 상품 카테고리
	 * @param pageable 페이지 정보
	 * @return 경매 페이지
	 */
	@Query("SELECT a FROM Auction a JOIN a.product p WHERE p.category = :category AND a.deletedAt IS NULL")
	Page<Auction> findByCategory(
		@Param("category") org.com.drop.domain.auction.product.entity.Product.Category category,
		Pageable pageable
	);

	/**
	 * 서브 카테고리별 경매 목록 조회
	 *
	 * @param subCategory 상품 서브 카테고리
	 * @param pageable 페이지 정보
	 * @return 경매 페이지
	 */
	@Query("SELECT a FROM Auction a JOIN a.product p WHERE p.subcategory = :subCategory AND a.deletedAt IS NULL")
	Page<Auction> findBySubCategory(
		@Param("subCategory") org.com.drop.domain.auction.product.entity.Product.SubCategory subCategory,
		Pageable pageable
	);

	/**
	 * LIVE 상태의 특정 시간 이후 종료되는 경매 조회
	 *
	 * @param endTime 종료 시간 기준
	 * @return 경매 목록
	 */
	@Query("SELECT a FROM Auction a WHERE a.status = 'LIVE' AND a.endAt <= :endTime AND a.deletedAt IS NULL")
	List<Auction> findLiveAuctionsEndingBefore(@Param("endTime") LocalDateTime endTime);

	/**
	 * 상품 ID로 경매 조회
	 *
	 * @param productId 상품 ID
	 * @return 경매 Optional
	 */
	@Query("SELECT a FROM Auction a WHERE a.product.id = :productId AND a.deletedAt IS NULL")
	Optional<Auction> findByProductId(@Param("productId") Long productId);

	/**
	 * 다중 ID로 경매 목록 조회
	 *
	 * @param ids 경매 ID 목록
	 * @return 경매 목록
	 */
	@Query("SELECT a FROM Auction a WHERE a.id IN :ids AND a.deletedAt IS NULL")
	List<Auction> findByIds(@Param("ids") List<Long> ids);

	/**
	 * 특정 상태의 경매 개수 조회
	 *
	 * @param status 경매 상태
	 * @return 경매 개수
	 */
	long countByStatus(Auction.AuctionStatus status);

	/**
	 * 경매 시작가 조회
	 *
	 * @param auctionId 경매 ID
	 * @return 시작가 Optional
	 */
	@Query("SELECT a.startPrice FROM Auction a WHERE a.id = :auctionId AND a.deletedAt IS NULL")
	Optional<Integer> findAuctionStartPrice(@Param("auctionId") Long auctionId);
}
