package org.com.drop.domain.payment.payment.service;

import org.com.drop.domain.payment.method.service.PaymentMethodService;
import org.com.drop.domain.payment.payment.domain.Payment;
import org.com.drop.domain.payment.payment.domain.PaymentStatus;
import org.com.drop.domain.payment.payment.dto.PaymentPrepareResponse;
import org.com.drop.domain.winner.domain.Winner;
import org.com.drop.domain.winner.repository.WinnerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentPrepareService {

	private final PaymentService paymentService;
	private final PaymentMethodService paymentMethodService;
	private final WinnerRepository winnerRepository;

	public PaymentPrepareResponse prepare(Long userId, Long winnerId) {

		Winner winner = winnerRepository.findById(winnerId)
			.orElseThrow(() ->
				new IllegalArgumentException("Winner not found")
			);

		Payment payment = paymentService.createPayment(
			winnerId,
			winner.getFinalPrice()
		);

		// 자동결제 시도는 이벤트에서 이미 처리됨
		if (payment.getStatus() == PaymentStatus.PAID) {
			return PaymentPrepareResponse.autoPaid(payment.getId());
		}

		// 카드 없거나 자동결제 실패 → 수동 결제
		return PaymentPrepareResponse.manualPay(
			payment.getId(),
			winner.getFinalPrice()
		);
	}
}
