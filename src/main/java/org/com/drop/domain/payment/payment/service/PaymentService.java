package org.com.drop.domain.payment.payment.service;

import org.com.drop.domain.payment.payment.domain.Payment;

public interface PaymentService {

	Payment createPayment(Long winnersId, Long amount);
	Payment attemptAutoPayment(Long paymentId, String billingKey);
	Payment confirmPaymentByWebhook(String paymentKey, Long winnersId, Long amount);
	Payment failPayment(Long paymentId, String reason);
}
