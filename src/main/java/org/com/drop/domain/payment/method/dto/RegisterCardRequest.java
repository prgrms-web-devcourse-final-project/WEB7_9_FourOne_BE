package org.com.drop.domain.payment.method.dto;

import org.com.drop.domain.payment.payment.domain.CardCompany;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RegisterCardRequest(

	@NotBlank(message = "USER_PAYMENT_METHOD_INVALID_BILLING_KEY")
	String billingKey,

	@NotNull(message = "USER_PAYMENT_METHOD_INVALID_CARD_COMPANY")
	CardCompany cardCompany,

	@NotBlank(message = "USER_PAYMENT_METHOD_INVALID_CARD_NUMBER_MASKED")
	@Pattern(
		regexp = "\\d{4}-\\*{4}-\\*{4}-\\d{4}",
		message = "USER_PAYMENT_METHOD_INVALID_CARD_NUMBER_MASKED"
	)
	String cardNumberMasked,

	@NotBlank(message = "USER_PAYMENT_METHOD_INVALID_CARD_NAME")
	String cardName
) {
}

