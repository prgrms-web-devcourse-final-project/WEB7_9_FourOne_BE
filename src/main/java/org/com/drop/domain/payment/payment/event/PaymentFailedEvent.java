package org.com.drop.domain.payment.payment.event;

public record PaymentFailedEvent(
	Long paymentId,
	String reason
) {
}
