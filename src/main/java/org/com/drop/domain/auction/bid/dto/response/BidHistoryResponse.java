package org.com.drop.domain.auction.bid.dto.response;

import java.time.LocalDateTime;

import org.com.drop.domain.auction.bid.entity.Bid;

import lombok.Builder;

@Builder
public record BidHistoryResponse(
	Long bidId,
	Long bidAmount,
	String bidder,
	LocalDateTime bidTime
) {
	public static BidHistoryResponse from(Bid bid) {
		return BidHistoryResponse.builder()
			.bidId(bid.getId())
			.bidAmount(bid.getBidAmount())
			.bidder(maskingName(bid.getBidder().getNickname()))
			.bidTime(bid.getCreatedAt())
			.build();
	}

	// 간단한 마스킹 헬퍼 메서드
	private static String maskingName(String name) {
		if (name == null || name.length() < 2) {
			return "**";
		}
		return name.substring(0, 2) + "***";
	}
}
