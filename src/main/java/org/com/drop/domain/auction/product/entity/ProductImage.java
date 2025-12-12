package org.com.drop.domain.auction.product.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "product_images",
	indexes = {@Index(name = "idx_product_id", columnList = "product")}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProductImage {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(nullable = false)
	private String imageUrl;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pre_img_id")
	private ProductImage preImg;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tail_img_id")
	private ProductImage trailImg;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public ProductImage(Product product, String imageUrl) {
		this.product = product;
		this.imageUrl = imageUrl;
	}

	public void setHead(ProductImage prev) {
		this.preImg = prev;
	}

	public void setTail(ProductImage trail) {
		this.trailImg = trail;
	}
}
