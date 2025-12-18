package org.com.drop.domain.payment.payment.infra.toss.dto;

import lombok.Builder;

@Builder
public record TossAutoPayRequest(
	Long amount,
	String customerKey,
	String orderId,
	String orderName,
	String customerEmail,
	String customerName,
	String customerMobilePhone
) {
}
