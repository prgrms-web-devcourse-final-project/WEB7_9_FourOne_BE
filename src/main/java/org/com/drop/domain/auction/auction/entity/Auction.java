package org.com.drop.domain.auction.auction.entity;

import java.time.LocalDateTime;

import org.com.drop.domain.auction.product.entity.Product;

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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "auctions", indexes = {@Index(name = "idx_auction_product_id", columnList = "productId"),
    @Index(name = "idx_auction_start_at", columnList = "startAt"),
    @Index(name = "idx_auction_end_at", columnList = "endAt"),
    @Index(name = "idx_auction_status", columnList = "status")})
@Getter
@Setter
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
    private Integer midBidStep;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionStatus status;

    private LocalDateTime deletedAt;

    public enum AuctionStatus {SCHEDULED, LIVE, ENDED, CANCELLED}

}
