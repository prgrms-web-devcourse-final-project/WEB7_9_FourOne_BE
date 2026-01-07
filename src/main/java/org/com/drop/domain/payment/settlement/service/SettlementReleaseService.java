package org.com.drop.domain.payment.settlement.service;

public interface SettlementReleaseService {
	void releaseByPurchaseConfirm(Long paymentId);

	void releaseAutomatically();
}

