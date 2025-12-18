package org.com.drop.domain.payment.payment.service;

public interface UserPaymentMethodService {

	UserPaymentInfo getUserPaymentInfo(Long userId);

	record UserPaymentInfo(
		boolean autoPayEnabled,
		String billingKey
	) {
	}
}
