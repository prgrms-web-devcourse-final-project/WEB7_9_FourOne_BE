package org.com.drop.domain.auction.auction.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record AuctionCreateRequest(

	@NotNull
	Long product_id,

	@NotNull
	@PositiveOrZero
	Integer startPrice,

	@NotNull
	@Positive
	Integer buyNowPrice,

	@NotNull
	@Positive
	Integer midBidStep,

	@NotNull
	@FutureOrPresent
	LocalDateTime startAt,

	@NotNull
	@Future
	LocalDateTime endAt
) { }

