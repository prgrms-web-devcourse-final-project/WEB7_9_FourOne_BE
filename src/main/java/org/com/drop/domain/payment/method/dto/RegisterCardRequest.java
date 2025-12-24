package org.com.drop.domain.payment.method.dto;

import org.com.drop.domain.payment.payment.domain.CardCompany;

public record RegisterCardRequest(
	String billingKey,
	CardCompany cardCompany,
	String cardNumberMasked,
	String cardName
) {
}
