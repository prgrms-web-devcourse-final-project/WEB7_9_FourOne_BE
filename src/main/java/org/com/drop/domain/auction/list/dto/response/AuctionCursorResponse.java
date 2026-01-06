package org.com.drop.domain.auction.list.dto.response;

import java.util.List;

/**
 * 경매 목록 커서 응답 DTO
 */
public record AuctionCursorResponse(
	List<AuctionItemResponse> items,
	String cursor,
	boolean hasNext
) {

	public static AuctionCursorResponse of(
		final List<AuctionItemResponse> items,
		final String cursor,
		final boolean hasNext
	) {
		return new AuctionCursorResponse(items, cursor, hasNext);
	}
}
