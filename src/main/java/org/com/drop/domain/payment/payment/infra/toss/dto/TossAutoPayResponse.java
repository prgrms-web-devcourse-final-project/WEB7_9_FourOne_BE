package org.com.drop.domain.payment.payment.infra.toss.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossAutoPayResponse(
	String paymentKey,
	String orderId,
	String status,
	String approvedAt,
	Long totalAmount,
	Card card,
	String method
) {
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Card(
		String issuerCode,
		String acquirerCode,
		String number,
		String cardType,
		String ownerType
	) {};
}
