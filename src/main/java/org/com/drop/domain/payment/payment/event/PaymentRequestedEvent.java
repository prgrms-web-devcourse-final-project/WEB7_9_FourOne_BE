package org.com.drop.domain.payment.payment.event;

public record PaymentRequestedEvent(
	Long winnerId,
	Long userId,
	Long amount
) {
}
