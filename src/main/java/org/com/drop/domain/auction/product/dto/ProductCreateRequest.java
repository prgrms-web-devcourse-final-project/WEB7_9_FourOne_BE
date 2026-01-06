package org.com.drop.domain.auction.product.dto;

import java.util.List;

import org.com.drop.domain.auction.product.entity.Product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductCreateRequest(
	@NotBlank(message = "PRODUCT_INVALID_PRODUCT_NAME")
	@Size(max = 100)
	String name,

	@NotBlank(message = "PRODUCT_INVALID_PRODUCT_DESCRIPTION")
	@Size(max = 1000)
	String description,

	@NotNull(message = "PRODUCT_INVALID_PRODUCT_CATEGORY")
	Product.Category category,

	@NotNull(message = "PRODUCT_INVALID_PRODUCT_SUB_CATEGORY")
	Product.SubCategory subCategory,

	@NotEmpty(message = "PRODUCT_INVALID_PRODUCT_IMAGE")
	@Size(max = 10)
	List<@NotBlank String> imagesFiles
) {
}
