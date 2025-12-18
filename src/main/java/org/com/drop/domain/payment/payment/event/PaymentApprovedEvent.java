package org.com.drop.domain.payment.payment.event;

public record PaymentApprovedEvent(
	String paymentKey,
	Long paymentId
) {
}
