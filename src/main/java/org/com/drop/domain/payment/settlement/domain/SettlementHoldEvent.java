package org.com.drop.domain.payment.settlement.domain;

public record SettlementHoldEvent(
	Long paymentId,
	Long sellerId,
	Long amount
) { }
