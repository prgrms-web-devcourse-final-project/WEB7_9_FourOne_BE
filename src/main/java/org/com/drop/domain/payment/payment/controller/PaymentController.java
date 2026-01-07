package org.com.drop.domain.payment.payment.controller;

import java.util.Map;

import org.com.drop.domain.payment.payment.service.PaymentService;
import org.com.drop.domain.user.entity.User;
import org.com.drop.global.rsdata.RsData;
import org.com.drop.global.security.auth.LoginUser;
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
	public RsData<?> create(
		@LoginUser User user,
		@RequestParam Long winnerId,
		@RequestParam Long amount
	) {
		return new RsData<>(
			paymentService.createPayment(winnerId, amount)
		);
	}

	@PostMapping("/{paymentId}/fail")
	public RsData<?> fail(
		@LoginUser User user,
		@PathVariable Long paymentId,
		@RequestBody Map<String, String> body
	) {
		return new RsData<>(
			paymentService.failPayment(paymentId, body.get("reason"))
		);
	}
}

