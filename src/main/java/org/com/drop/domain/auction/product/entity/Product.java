package org.com.drop.domain.auction.product.entity;

import java.time.LocalDateTime;

import org.com.drop.domain.user.entity.User;

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

@Entity
@Table(name = "products", indexes = {@Index(name = "idx_product_category", columnList = "category"),
	@Index(name = "idx_product_subcategory", columnList = "subcategory"),
	@Index(name = "idx_product_deleted_at", columnList = "deleted_at")})
@Getter
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

	public enum Category { STARGOODS, FIGURE, CDLP, GAME }

	public enum SubCategory { ACC, STATIONARY, DAILY, ETC, ELECTRONICS, GAME }

	public Product(
		User seller,
		String name,
		String description,
		Category category,
		SubCategory subcategory) {
		this.seller = seller;
		this.name = name;
		this.description = description;
		this.category = category;
		this.subcategory = subcategory;
		this.createdAt = LocalDateTime.now();
		this.bookmarkCount = 0;
	}

	public void update(String name, String description, Category category, SubCategory subcategory) {
		this.name = name;
		this.description = description;
		this.category = category;
		this.subcategory = subcategory;
		this.updatedAt = LocalDateTime.now();
	}
}
