package org.com.drop.domain.auction.list.dto.response;

import java.util.List;

/**
 * 홈화면 응답 DTO
 */
public record AuctionHomeResponse(
	List<AuctionItemResponse> endingSoon,
	List<AuctionItemResponse> popular
) { }
