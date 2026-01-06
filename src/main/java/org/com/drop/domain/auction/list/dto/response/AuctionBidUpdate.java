package org.com.drop.domain.auction.list.dto.response;

/**
 * SSE 입찰 업데이트 DTO
 */
public record AuctionBidUpdate(
	Integer currentHighestBid,
	String bidderNickname
) { }
