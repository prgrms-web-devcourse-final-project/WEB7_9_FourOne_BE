package org.com.drop.domain.auction.auction.entity;

import java.time.LocalDateTime;

import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.global.exception.ErrorCode;
import org.com.drop.global.exception.ServiceException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auctions", indexes = {@Index(name = "idx_auction_product_id", columnList = "productId"),
	@Index(name = "idx_auction_start_at", columnList = "startAt"),
	@Index(name = "idx_auction_end_at", columnList = "endAt"),
	@Index(name = "idx_auction_status", columnList = "status")})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Auction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(nullable = false)
	private Integer startPrice;

	private Integer buyNowPrice;

	@Column(nullable = false)
	private Integer currentPrice;

	@Column(nullable = false)
	private Integer minBidStep;

	@Column(nullable = false)
	private LocalDateTime startAt;

	@Column(nullable = false)
	private LocalDateTime endAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AuctionStatus status;

	private LocalDateTime deletedAt;

	public enum AuctionStatus { SCHEDULED, LIVE, ENDED, CANCELLED }

	public Auction(
		Product product,
		Integer startPrice,
		Integer buyNowPrice,
		Integer minBidStep,
		LocalDateTime startAt,
		LocalDateTime endAt,
		AuctionStatus status) {
		this.product = product;
		this.startPrice = startPrice;
		this.buyNowPrice = buyNowPrice;
		this.currentPrice = startPrice;
		this.minBidStep = minBidStep;
		this.startAt = startAt;
		this.endAt = endAt;
		this.status = status;
	}

	public void end(LocalDateTime now) {
		if (this.status != AuctionStatus.LIVE) {
			throw new ServiceException(ErrorCode.AUCTION_NOT_LIVE, "진행 중인 경매가 아닙니다.");
		}
		this.status = AuctionStatus.ENDED;
		this.endAt = now;
	}

	public void start(LocalDateTime now) {
		if (this.status == AuctionStatus.SCHEDULED) {
			this.status = AuctionStatus.LIVE;
		}
	}

	public void updateCurrentPrice(Long bidAmount) {
		this.currentPrice = bidAmount.intValue();
	}

	@PrePersist
	public void ensureCurrentPrice() {
		// currentPrice가 비어있다면, startPrice로 강제 세팅
		if (this.currentPrice == null) {
			this.currentPrice = this.startPrice;
		}
	}
}
