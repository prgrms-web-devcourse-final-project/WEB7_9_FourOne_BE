package org.com.drop.domain.payment.payment.repository;

import org.com.drop.domain.payment.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

	Optional<Payment> findByTossPaymentKey(String tossPaymentKey);

	Optional<Payment> findByWinnersId(Long winnersId);
}
