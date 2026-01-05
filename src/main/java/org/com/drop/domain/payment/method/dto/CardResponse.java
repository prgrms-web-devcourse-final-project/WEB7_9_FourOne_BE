package org.com.drop.domain.payment.method.dto;

import java.time.LocalDateTime;

import org.com.drop.domain.payment.method.entity.PaymentMethod;
import org.com.drop.domain.payment.payment.domain.CardCompany;

public record CardResponse(
	Long id,
	String billingKey,
	CardCompany cardCompany,
	String cardNumberMasked,
	LocalDateTime createdAt
) {
	public CardResponse(PaymentMethod paymentMethod) {
		this(
			paymentMethod.getId(),
			paymentMethod.getBillingKey(),
			paymentMethod.getCardCompany(),
			paymentMethod.getCardNumberMasked(),
			paymentMethod.getCreatedAt()
		);
	}
}
