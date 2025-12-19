package org.com.drop.domain.payment.payment.infra.toss;

import org.com.drop.domain.payment.payment.infra.toss.dto.TossAutoPayRequest;
import org.com.drop.domain.payment.payment.infra.toss.dto.TossAutoPayResponse;

public interface TossPaymentsClient {
	TossAutoPayResponse approveBilling(String billingKey, TossAutoPayRequest request, String idempotencyKey);
}
