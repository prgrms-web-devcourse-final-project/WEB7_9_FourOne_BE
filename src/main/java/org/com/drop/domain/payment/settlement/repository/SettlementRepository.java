package org.com.drop.domain.payment.settlement.repository;

import org.com.drop.domain.payment.settlement.domain.Settlement;
import org.com.drop.domain.payment.settlement.domain.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

	Optional<Settlement> findByPaymentId(Long paymentId);

	List<Settlement> findAllByStatusAndHoldAtBefore(
		SettlementStatus status,
		LocalDateTime 기준시간
	);
}
