package org.com.drop.domain.payment.settlement.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.com.drop.domain.payment.settlement.domain.Settlement;
import org.com.drop.domain.payment.settlement.domain.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

	Optional<Settlement> findByPaymentId(Long paymentId);

	List<Settlement> findAllByStatusAndHoldAtBefore(
		SettlementStatus status,
		LocalDateTime baseTime
	);
}
