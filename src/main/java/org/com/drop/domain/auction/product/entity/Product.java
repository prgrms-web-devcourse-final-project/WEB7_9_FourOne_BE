package org.com.drop.domain.auction.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.com.drop.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
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
    @Column(name = "seller_id", nullable = false)
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

    @Column
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Integer bookmarkCount;

    @Column
    private LocalDateTime deletedAt;

    // --- Enum 정의 ---
    public enum Category {
        STARGOODS, FIGURE, CDLP, GAME
    }

    public enum SubCategory {
        ACC, STATIONARY, DAILY, ETC, ELECTRONICS, GAME
    }

    // --- Lifecycle Hooks ---
    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.bookmarkCount == null) this.bookmarkCount = 0;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
