package org.com.drop.domain.auction.product.entity;

import jakarta.persistence.*;
import lombok.*;

import org.com.drop.domain.user.entity.User;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "products",
    indexes = {
        @Index(name = "idx_product_category", columnList = "category"),
        @Index(name = "idx_product_subcategory", columnList = "subcategory"),
        @Index(name = "idx_product_deleted_at", columnList = "deleted_at")
    }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    private SubCategory subcategory;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Integer bookmarkCount;

    private LocalDateTime deletedAt;

    public enum Category {STARGOODS, FIGURE, CDLP, GAME}

    public enum SubCategory {ACC, STATIONARY, DAILY, ETC, ELECTRONICS, GAME}
}
