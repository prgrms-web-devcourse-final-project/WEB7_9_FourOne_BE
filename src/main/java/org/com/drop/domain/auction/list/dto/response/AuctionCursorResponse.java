package org.com.drop.domain.auction.list.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 경매 목록 커서 응답 DTO
 */
@Getter
@Builder
public class AuctionCursorResponse {
	private final String cursor;
	private final List<AuctionItemResponse> items;
	private final boolean hasNext;

	public static AuctionCursorResponse of(
		final List<AuctionItemResponse> items,
		final String cursor,
		final boolean hasNext
	) {
		return AuctionCursorResponse.builder()
			.items(items)
			.cursor(cursor)
			.hasNext(hasNext)
			.build();
	}
}
