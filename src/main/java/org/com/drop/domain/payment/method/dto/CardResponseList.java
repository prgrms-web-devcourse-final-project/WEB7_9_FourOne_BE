package org.com.drop.domain.payment.method.dto;

import java.util.List;

import org.com.drop.domain.payment.method.entity.PaymentMethod;

public record CardResponseList(
	List<CardResponse> registerCardResponses
) {
	public static CardResponseList from(List<PaymentMethod> paymentMethods) {
		return new CardResponseList(
			paymentMethods.stream()
				.map(CardResponse::new)
				.toList()
		);
	}
}
