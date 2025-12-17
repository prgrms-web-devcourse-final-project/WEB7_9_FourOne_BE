package org.com.drop.domain.payment.payment.event;

public record WinnerConfirmedEvent(
	Long winnerId,
	Long userId,
	Long finalPrice
) {}
