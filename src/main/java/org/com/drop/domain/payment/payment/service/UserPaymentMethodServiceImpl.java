package org.com.drop.domain.payment.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPaymentMethodServiceImpl implements UserPaymentMethodService {

	@Override
	public UserPaymentInfo getUserPaymentInfo(Long userId) {
		return new UserPaymentInfo(
			true,
			"test_billingKey"
		);
	}
}
