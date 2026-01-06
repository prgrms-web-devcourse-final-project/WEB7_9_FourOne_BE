package org.com.drop.domain.payment.payment.controller;

import org.com.drop.domain.payment.payment.dto.PaymentPrepareRequest;
import org.com.drop.domain.payment.payment.dto.PaymentPrepareResponse;
import org.com.drop.domain.payment.payment.service.PaymentPrepareService;
import org.com.drop.domain.user.entity.User;
import org.com.drop.global.rsdata.RsData;
import org.com.drop.global.security.auth.LoginUser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentPrepareController {

	private final PaymentPrepareService paymentPrepareService;

	@PostMapping("/prepare")
	public RsData<PaymentPrepareResponse> prepare(
		@LoginUser User user,
		@RequestBody PaymentPrepareRequest request
	) {
		PaymentPrepareResponse response =
			paymentPrepareService.prepare(
				user.getId(),
				request.winnerId()
			);

		return new RsData<>(response);
	}
}
