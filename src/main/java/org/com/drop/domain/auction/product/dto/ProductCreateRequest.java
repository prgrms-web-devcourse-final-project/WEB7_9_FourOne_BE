package org.com.drop.domain.auction.product.dto;

import java.util.List;

import org.com.drop.domain.auction.product.entity.Product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductCreateRequest(
	@NotBlank
	@Size(max = 100)
	String name,

	@NotBlank
	@Size(max = 1000)
	String description,

	@NotNull
	Product.Category category,

	@NotNull
	Product.SubCategory subCategory,

	@NotEmpty
	@Size(max = 10)
	List<@NotBlank String> imagesFiles
) {
}
