package org.com.drop.domain.payment.payment.infra.toss.webhook.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossWebhookRequest(
	String eventType,
	Data data
) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Data(
		String paymentKey,
		String orderId,
		String status,
		Long totalAmount
	) {};
}
