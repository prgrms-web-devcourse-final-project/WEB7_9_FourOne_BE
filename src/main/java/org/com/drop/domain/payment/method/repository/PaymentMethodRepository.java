package org.com.drop.domain.payment.method.repository;

import java.util.List;

import org.com.drop.domain.payment.method.domain.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
	List<PaymentMethod> findByUserId(Long userId);
}
