package org.com.drop.domain.payment.payment.dto;

import org.com.drop.domain.payment.payment.domain.PaymentStatus;

public record PaymentPrepareResponse(
	Long paymentId,
	PaymentStatus status,
	boolean autoPaid,
	TossInfo toss
) {

	public static PaymentPrepareResponse autoPaid(Long paymentId) {
		return new PaymentPrepareResponse(
			paymentId,
			PaymentStatus.PAID,
			true,
			null
		);
	}

	public static PaymentPrepareResponse manualPay(
		Long paymentId,
		Long amount
	) {
		return new PaymentPrepareResponse(
			paymentId,
			PaymentStatus.REQUESTED,
			false,
			new TossInfo(
				"payment-" + paymentId,
				amount
			)
		);
	}

	public record TossInfo(
		String orderId,
		Long amount
	) {
	}
}
