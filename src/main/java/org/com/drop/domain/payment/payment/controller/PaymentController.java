package org.com.drop.domain.payment.payment.controller;

import java.util.Map;

import org.com.drop.domain.payment.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

	private final PaymentService paymentService;

	@PostMapping("/create")
	public ResponseEntity<?> create(@RequestParam Long winnersId,
		@RequestParam Long amount) {
		return ResponseEntity.ok(paymentService.createPayment(winnersId, amount));
	}

	@PostMapping("/approve/webhook")
	public ResponseEntity<?> approveByWebhook(
		@RequestParam String tossPaymentKey,
		@RequestParam Long winnersId,
		@RequestParam Long amount
	) {
		return ResponseEntity.ok(
			paymentService.confirmPaymentByWebhook(tossPaymentKey, winnersId, amount)
		);
	}

	@PostMapping("/{paymentId}/fail")
	public ResponseEntity<?> fail(
		@PathVariable Long paymentId,
		@RequestBody Map<String, String> body
	) {
		return ResponseEntity.ok(paymentService.failPayment(paymentId, body.get("reason")));
	}
}
