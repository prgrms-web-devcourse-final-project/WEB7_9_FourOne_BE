package org.com.drop.domain.auction.product.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.com.drop.domain.auction.product.entity.Product;

public record ProductSearchResponse(
	Long productId,
	Long sellerId,
	String name,
	String description,
	List<String> images,
	Product.Category category,
	Product.SubCategory subCategory,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	int bookmarkCount
) {
	public ProductSearchResponse(Product product, List<String> imgs) {
		this(
			product.getId(),
			product.getSeller().getId(),
			product.getName(),
			product.getDescription(),
			imgs,
			product.getCategory(),
			product.getSubcategory(),
			product.getCreatedAt(),
			product.getUpdatedAt(),
			product.getBookmarkCount()
		);
	}
}
