package org.com.drop.domain.payment.payment.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
public class PaymentRedirectController {

	@GetMapping("/payments/redirect/fail")
	public String failRedirect(
		@RequestParam(required = false) String code,
		@RequestParam(required = false) String message,
		@RequestParam(required = false) String orderId
	) {
		log.info("[PAYMENT FAIL REDIRECT] code={}, message={}, orderId={}",
			code, message, orderId);
		return "redirect:/app/payments/fail"
			+ "?code=" + safe(code)
			+ "&message=" + safe(message)
			+ "&orderId=" + safe(orderId);
	}

	private String safe(String value) {
		return value == null ? "" : value;
	}
}
