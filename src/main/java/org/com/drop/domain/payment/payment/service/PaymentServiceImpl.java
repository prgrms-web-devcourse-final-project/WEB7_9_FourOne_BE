package org.com.drop.domain.payment.payment.service;

import java.util.concurrent.TimeUnit;

import org.com.drop.domain.payment.payment.domain.Payment;
import org.com.drop.domain.payment.payment.domain.PaymentStatus;
import org.com.drop.domain.payment.payment.infra.toss.TossPaymentsClient;
import org.com.drop.domain.payment.payment.infra.toss.dto.TossAutoPayRequest;
import org.com.drop.domain.payment.payment.infra.toss.dto.TossAutoPayResponse;
import org.com.drop.domain.payment.payment.infra.toss.util.CustomerKeyGenerator;
import org.com.drop.domain.payment.payment.repository.PaymentRepository;
import org.com.drop.domain.payment.settlement.domain.Settlement;
import org.com.drop.domain.payment.settlement.repository.SettlementRepository;
import org.com.drop.domain.winner.domain.Winner;
import org.com.drop.domain.winner.repository.WinnerRepository;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

	private final PaymentRepository paymentRepository;
	private final SettlementRepository settlementRepository;
	private final TossPaymentsClient tossPaymentsClient;
	private final CustomerKeyGenerator customerKeyGenerator;
	private final WinnerRepository winnerRepository;

	private final RedissonClient redissonClient;

	@Override
	@Transactional
	public Payment createPayment(Long winnersId, Long amount) {
		Winner winner = winnerRepository.findById(winnersId)
			.orElseThrow(() -> new RuntimeException("Winner not found"));

		Payment payment = Payment.builder()
			.winnersId(winnersId)
			.sellersId(winner.getSellerId())
			.status(PaymentStatus.REQUESTED)
			.fee(Math.round(amount * 0.05))
			.net(amount)
			.build();

		return paymentRepository.save(payment);
	}

	@Override
	public Payment attemptAutoPayment(Long paymentId, String billingKey) {
		RLock lock = redissonClient.getLock("PAYMENT_LOCK:" + paymentId);

		try {
			if (!lock.tryLock(5, 20, TimeUnit.SECONDS)) {
				log.warn("[LOCK-FAILED] 이미 결제가 진행 중입니다. paymentId={}", paymentId);
				throw new RuntimeException("결제가 진행 중입니다. 잠시만 기다려주세요.");
			}

			return proceedPaymentWithCircuitBreaker(paymentId, billingKey);

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("결제 처리 중 오류가 발생했습니다.");
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	@CircuitBreaker(name = "tossPayment", fallbackMethod = "handleTossFailure")
	@Transactional
	public Payment proceedPaymentWithCircuitBreaker(Long paymentId, String billingKey) {
		Payment payment = paymentRepository.findById(paymentId)
			.orElseThrow(() -> new RuntimeException("Payment not found"));

		if (payment.getStatus() == PaymentStatus.PAID || payment.getStatus() == PaymentStatus.PROCESSING) {
			log.info("[ALREADY-PROCESSED] paymentId={} status={}", paymentId, payment.getStatus());
			return payment;
		}

		payment.markProcessing();
		paymentRepository.saveAndFlush(payment);

		log.info("[AUTO-PAY] start paymentId={}, billingKey={}", paymentId, billingKey);

		try {
			String customerKey = customerKeyGenerator.generate("winner:" + payment.getWinnersId());
			TossAutoPayRequest request = TossAutoPayRequest.builder()
				.amount(payment.getNet())
				.customerKey(customerKey)
				.orderId("payment-" + payment.getId())
				.orderName("DROP 경매 자동결제")
				.build();

			String idempotencyKey = "auto-pay-" + payment.getId();

			TossAutoPayResponse response = tossPaymentsClient.approveBilling(billingKey, request, idempotencyKey);

			if ("DONE".equalsIgnoreCase(response.status())) {
				payment.assignTossPaymentKey(response.paymentKey());
				payment.markPaid();
				createSettlement(payment);
				log.info("[AUTO-PAY] success paymentId={}", paymentId);
			} else {
				payment.markFailed();
				log.warn("[AUTO-PAY] failed paymentId={}, status={}", paymentId, response.status());
			}

		} catch (Exception e) {
			log.error("[AUTO-PAY] API call exception paymentId={}", paymentId, e);
			payment.markFailed();
			throw e;
		}

		return payment;
	}

	public Payment handleTossFailure(Long paymentId, String billingKey, Throwable t) {
		log.error("[CIRCUIT-BREAKER] 토스 API 호출 차단됨. 원인: {}", t.getMessage());
		throw new RuntimeException("현재 결제 서비스 점검 중입니다. 잠시 후 다시 시도해주세요.");
	}

	@Override
	public Payment confirmPaymentByWebhook(
		String paymentKey,
		Long winnersId,
		Long amount
	) {
		RLock lock = redissonClient.getLock("PAYMENT_WEBHOOK_LOCK:" + winnersId);

		try {
			if (!lock.tryLock(5, 15, TimeUnit.SECONDS)) {
				log.warn("[WEBHOOK-LOCK-FAILED] winnersId={}", winnersId);
				throw new RuntimeException("Lock acquisition failed");
			}

			return processWebhookLogic(paymentKey, winnersId, amount);

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Webhook processing interrupted", e);
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	@Transactional
	public Payment processWebhookLogic(String paymentKey, Long winnersId, Long amount) {
		Payment payment = paymentRepository.findByWinnersId(winnersId)
			.orElseThrow(() -> new RuntimeException("Payment not found"));

		if (payment.getStatus() == PaymentStatus.PAID) {
			return payment;
		}

		if (!payment.getNet().equals(amount)) {
			throw new IllegalArgumentException("Payment amount mismatch");
		}

		payment.assignTossPaymentKey(paymentKey);
		payment.markPaid();
		createSettlement(payment);

		return paymentRepository.save(payment);
	}

	@Override
	@Transactional
	public Payment failPayment(Long paymentId, String reason) {

		Payment payment = paymentRepository.findById(paymentId)
			.orElseThrow(() -> new RuntimeException("Payment not found"));

		payment.markFailed();

		return payment;
	}

	private void createSettlement(Payment payment) {

		if (settlementRepository.findByPaymentId(payment.getId()).isPresent()) {
			log.info("[SETTLEMENT IDEMPOTENT] already exists. paymentId={}", payment.getId());
			return;
		}

		Settlement settlement = Settlement.builder()
			.paymentId(payment.getId())
			.sellerId(payment.getSellersId())
			.fee(payment.getFee())
			.net(payment.getNet())
			.build();

		settlementRepository.save(settlement);
	}
}
