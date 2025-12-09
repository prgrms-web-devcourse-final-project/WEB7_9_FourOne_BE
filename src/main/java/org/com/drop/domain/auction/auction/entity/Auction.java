package org.com.drop.domain.auction.auction.entity;

import jakarta.persistence.*;
import lombok.*;

import org.com.drop.domain.auction.product.entity.Product;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "auctions",
    indexes = {
        @Index(name = "idx_auction_product_id", columnList = "productId"),
        @Index(name = "idx_auction_start_at", columnList = "startAt"),
        @Index(name = "idx_auction_end_at", columnList = "endAt"),
        @Index(name = "idx_auction_status", columnList = "status")
    }
)
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
