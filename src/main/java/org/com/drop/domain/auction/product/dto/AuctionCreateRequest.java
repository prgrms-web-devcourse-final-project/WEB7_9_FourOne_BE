package org.com.drop.domain.auction.product.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.com.drop.domain.auction.product.entity.Product;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AuctionCreateRequest(
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

	@NotNull
	@Min(0)
	Integer startPrice,

	@NotNull
	@Min(1)
	Integer minBidStep,

	@NotNull
	@Future
	LocalDateTime startAt,

	@NotNull
	@Future
	LocalDateTime endAt,

	@Min(0)
	Integer buyNowPrice,

	@NotEmpty
	@Size(max = 10)
	List<@NotBlank String> imagesFiles
) {
}
