package org.com.drop.domain.payment.settlement.service;

import org.com.drop.domain.payment.settlement.domain.Settlement;
import org.com.drop.domain.payment.settlement.domain.SettlementStatus;
import org.com.drop.domain.payment.settlement.repository.SettlementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementReleaseServiceImplTest {

	@Mock
	SettlementRepository settlementRepository;

	@InjectMocks
	SettlementReleaseServiceImpl service;

	@Test
	void releaseByPurchaseConfirm_marksSettlementPaid() {
		// given
		Long paymentId = 1L;

		Settlement settlement = Settlement.builder()
			.paymentId(paymentId)
			.sellerId(2L)
			.status(SettlementStatus.HOLDING)
			.build();

		when(settlementRepository.findByPaymentId(paymentId))
			.thenReturn(Optional.of(settlement));

		// when
		service.releaseByPurchaseConfirm(paymentId);

		// then
		assertThat(settlement.getStatus())
			.isEqualTo(SettlementStatus.PAID);
		assertThat(settlement.getPaidAt())
			.isNotNull();

		verify(settlementRepository).save(settlement);
	}
}


