package org.com.drop.domain.auction.list.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * SSE 입찰 업데이트 DTO
 */
@Getter
@Builder
public class AuctionBidUpdate {
	private final Integer currentHighestBid;
	private final String bidderNickname;
}
