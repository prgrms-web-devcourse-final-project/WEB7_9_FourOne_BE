package org.com.drop.domain.auction.auction.entity;

import jakarta.persistence.*;
import lombok.*;
import org.com.drop.domain.auction.product.entity.Product;

import java.time.LocalDateTime;

@Entity
@Table(name = "auctions")
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
    @Column(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer startPrice;

    @Column
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

    @Column
    private LocalDateTime deletedAt;

    public enum AuctionStatus {
        SCHEDULED, LIVE, ENDED, CANCELLED
    }

}
