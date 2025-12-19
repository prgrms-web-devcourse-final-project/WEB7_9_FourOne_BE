package org.com.drop.domain.payment.payment.event;

public record AutoPaymentRequestedEvent(
	Long paymentId,
	Long userId,
	String billingKey,
	Long amount
) {
}
