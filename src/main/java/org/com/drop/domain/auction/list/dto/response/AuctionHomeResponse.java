package org.com.drop.domain.auction.list.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 홈화면 응답 DTO
 */
@Getter
@Builder
public class AuctionHomeResponse {
	private final List<AuctionItemResponse> endingSoon;
	private final List<AuctionItemResponse> popular;
}
