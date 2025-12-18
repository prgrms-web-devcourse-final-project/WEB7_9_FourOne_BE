package org.com.drop.domain.payment.payment.infra.toss.webhook;

import org.com.drop.domain.payment.payment.infra.toss.webhook.dto.TossWebhookRequest;
import org.com.drop.domain.payment.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/webhooks/toss")
@RequiredArgsConstructor
public class TossWebhookController {

	private final PaymentService paymentService;

	@PostMapping
	public ResponseEntity<Void> handle(@RequestBody TossWebhookRequest request) {

		log.info("[TOSS WEBHOOK] received eventType={}", request.eventType());

		if (!"PAYMENT_STATUS_CHANGED".equals(request.eventType())) {
			log.info("[TOSS WEBHOOK] ignored eventType={}", request.eventType());
			return ResponseEntity.ok().build();
		}

		var data = request.data();
		if (!"DONE".equalsIgnoreCase(data.status())) {
			log.info("[TOSS WEBHOOK] payment not done. status={}", data.status());
			return ResponseEntity.ok().build();
		}

		try {
			paymentService.confirmPaymentByWebhook(
				data.paymentKey(),
				extractWinnersId(data.orderId()),
				data.totalAmount()
			);

			log.info("[TOSS WEBHOOK] payment approved. orderId={}", data.orderId());

		} catch (Exception e) {
			log.error("[TOSS WEBHOOK] error processing webhook", e);
		}

		return ResponseEntity.ok().build();
	}

	private Long extractWinnersId(String orderId) {
		try {
			return Long.parseLong(orderId.replace("payment-", ""));
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid orderId format: " + orderId);
		}
	}
}
