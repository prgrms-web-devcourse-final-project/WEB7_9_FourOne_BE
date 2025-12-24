package org.com.drop.domain.payment.method.controller;

import java.util.List;

import org.com.drop.domain.payment.method.dto.CardResponse;
import org.com.drop.domain.payment.method.dto.RegisterCardRequest;
import org.com.drop.domain.payment.method.service.PaymentMethodService;
import org.com.drop.domain.user.entity.User;
import org.com.drop.global.rsdata.RsData;
import org.com.drop.global.security.auth.LoginUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/payments/cards")
@RequiredArgsConstructor
public class PaymentMethodController {

	private final PaymentMethodService paymentMethodService;

	@PostMapping
	public RsData<Void> register(
		@LoginUser User user,
		@RequestBody RegisterCardRequest request
	) {
		paymentMethodService.registerCard(user.getId(), request);
		return new RsData<>(null);
	}

	@GetMapping
	public RsData<List<CardResponse>> list(@LoginUser User user) {
		return new RsData<>(paymentMethodService.getCards(user.getId()));
	}

	@DeleteMapping("/{cardId}")
	public RsData<Void> delete(
		@LoginUser User user,
		@PathVariable Long cardId
	) {
		paymentMethodService.deleteCard(user.getId(), cardId);
		return new RsData<>(null);
	}
}
