package org.com.drop.domain.payment.method.dto;

import org.com.drop.domain.payment.method.domain.PaymentMethod;
import org.com.drop.domain.payment.payment.domain.CardCompany;

public record CardResponse(
	Long id,
	CardCompany cardCompany,
	String cardNumberMasked,
	String cardName
) {
	public static CardResponse from(PaymentMethod method) {
		return new CardResponse(
			method.getId(),
			method.getCardCompany(),
			method.getCardNumberMasked(),
			method.getCardName()
		);
	}
}
