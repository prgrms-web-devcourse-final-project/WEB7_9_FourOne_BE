package org.com.drop.domain.payment.payment.infra.toss.webhook;

import org.com.drop.domain.payment.payment.infra.toss.webhook.dto.TossWebhookRequest;

public class TestWebhookFactory {

	private TestWebhookFactory() {
	}

	public static TossWebhookRequest doneEvent(Long winnersId, String paymentKey, Long totalAmount) {
		return new TossWebhookRequest(
			"PAYMENT_STATUS_CHANGED",
			new TossWebhookRequest.Data(
				paymentKey,
				"payment-" + winnersId,
				"DONE",
				totalAmount
			)
		);
	}

	public static TossWebhookRequest ignoredEvent() {
		return new TossWebhookRequest(
			"SOMETHING_ELSE",
			new TossWebhookRequest.Data(
				"pk_test",
				"payment-1",
				"DONE",
				1000L
			)
		);
	}

	public static TossWebhookRequest notDoneEvent() {
		return new TossWebhookRequest(
			"PAYMENT_STATUS_CHANGED",
			new TossWebhookRequest.Data(
				"pk_test",
				"payment-1",
				"WAITING_FOR_DEPOSIT",
				1000L
			)
		);
	}
}
