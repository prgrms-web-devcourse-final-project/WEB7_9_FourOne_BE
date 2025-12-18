package org.com.drop.domain.payment.payment.repository;

import java.util.Optional;

import org.com.drop.domain.payment.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findByTossPaymentKey(String tossPaymentKey);

	Optional<Payment> findByWinnersId(Long winnersId);
}
