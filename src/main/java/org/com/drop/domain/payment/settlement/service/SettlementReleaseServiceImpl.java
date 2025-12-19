package org.com.drop.domain.payment.settlement.service;

import java.time.LocalDateTime;
import java.util.List;

import org.com.drop.domain.payment.settlement.domain.Settlement;
import org.com.drop.domain.payment.settlement.domain.SettlementStatus;
import org.com.drop.domain.payment.settlement.repository.SettlementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementReleaseServiceImpl implements SettlementReleaseService {

	private final SettlementRepository settlementRepository;

	@Override
	@Transactional
	public void releaseByPurchaseConfirm(Long paymentId) {

		Settlement settlement = settlementRepository.findByPaymentId(paymentId)
			.orElseThrow(() -> new RuntimeException("Settlement not found"));

		if (settlement.getStatus() == SettlementStatus.PAID) {
			log.info("[SETTLEMENT] already released. paymentId={}", paymentId);
			return;
		}

		settlement.markPaid();

		log.info("[SETTLEMENT] released by purchase confirm. paymentId={}", paymentId);
	}

	@Override
	@Transactional
	public void releaseAutomatically() {

		LocalDateTime basetime = LocalDateTime.now().minusDays(7);

		List<Settlement> targets =
			settlementRepository.findAllByStatusAndHoldAtBefore(
				SettlementStatus.HOLDING,
				basetime
			);

		for (Settlement settlement : targets) {
			settlement.markPaid();
			log.info("[SETTLEMENT] auto released. settlementId={}", settlement.getId());
		}
	}
}

