package org.com.drop.domain.auction.product.dto;

import java.time.LocalDateTime;

import org.com.drop.domain.auction.product.entity.Product;

public record ProductCreateResponse(
	Long productId,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
	public ProductCreateResponse(Product product) {
		this(
			product.getId(),
			product.getCreatedAt(),
			product.getUpdatedAt()
		);
	}
}
