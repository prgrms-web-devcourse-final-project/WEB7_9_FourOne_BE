package org.com.drop.domain.payment.payment.service;

import java.time.LocalDateTime;

import org.com.drop.domain.payment.payment.domain.Payment;
import org.com.drop.domain.payment.payment.domain.PaymentStatus;
import org.com.drop.domain.payment.payment.infra.toss.TossPaymentsClient;
import org.com.drop.domain.payment.payment.infra.toss.dto.TossAutoPayRequest;
import org.com.drop.domain.payment.payment.infra.toss.dto.TossAutoPayResponse;
import org.com.drop.domain.payment.payment.infra.toss.util.CustomerKeyGenerator;
import org.com.drop.domain.payment.payment.repository.PaymentRepository;
import org.com.drop.domain.payment.settlement.domain.Settlement;
import org.com.drop.domain.payment.settlement.domain.SettlementStatus;
import org.com.drop.domain.payment.settlement.repository.SettlementRepository;
import org.com.drop.domain.winner.domain.Winner;
import org.com.drop.domain.winner.repository.WinnerRepository;
import org.com.drop.global.exception.AlreadyProcessedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

	private final PaymentRepository paymentRepository;
	private final SettlementRepository settlementRepository;
	private final TossPaymentsClient tossPaymentsClient;
	private final CustomerKeyGenerator customerKeyGenerator;
	private final WinnerRepository winnerRepository;

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
			.requestedAt(LocalDateTime.now())
			.build();

		return paymentRepository.save(payment);
	}

	@Override
	@Transactional
	public Payment attemptAutoPayment(Long paymentId, String billingKey) {

		Payment payment = paymentRepository.findById(paymentId)
			.orElseThrow(() -> new RuntimeException("Payment not found"));

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

			TossAutoPayResponse response =
				tossPaymentsClient.approveBilling(billingKey, request, idempotencyKey);

			log.info("[AUTO-PAY] Toss response paymentId={}, status={}",
				paymentId, response.status());
			if ("DONE".equalsIgnoreCase(response.status())) {

				payment.setStatus(PaymentStatus.PAID);
				payment.setTossPaymentKey(response.paymentKey());
				payment.setApprovedAt(LocalDateTime.now());

				createSettlement(payment);

				log.info("[AUTO-PAY] success paymentId={}", paymentId);

			} else {
				payment.setStatus(PaymentStatus.FAILED);
				payment.setReceipt("자동결제 실패: status=" + response.status());

				log.warn("[AUTO-PAY] failed paymentId={}, status={}",
					paymentId, response.status());
			}

		} catch (Exception e) {
			log.error("[AUTO-PAY] exception paymentId={}", paymentId, e);

			payment.setStatus(PaymentStatus.FAILED);
			payment.setReceipt("자동결제 예외: " + e.getMessage());
		}

		return paymentRepository.save(payment);
	}

	@Override
	@Transactional
	public Payment confirmPaymentByWebhook(String paymentKey, Long winnersId, Long amount) {
		paymentRepository.findByTossPaymentKey(paymentKey)
			.ifPresent(existing -> {
				log.info("[WEBHOOK IDEMPOTENT] already processed paymentKey={}", paymentKey);
				throw new AlreadyProcessedException();
			});
		Payment payment = paymentRepository.findByWinnersId(winnersId)
			.orElseThrow(() -> new RuntimeException("Payment not found by winnersId"));
		if (payment.getStatus() == PaymentStatus.PAID) {
			log.info("[WEBHOOK IDEMPOTENT] payment already PAID. paymentId={}", payment.getId());
			return payment;
		}
		if (!payment.getNet().equals(amount)) {
			log.error("[WEBHOOK INVALID] amount mismatch. expected={}, actual={}",
				payment.getNet(), amount);
			throw new IllegalArgumentException("Payment amount mismatch");
		}
		payment.setTossPaymentKey(paymentKey);
		payment.setStatus(PaymentStatus.PAID);
		payment.setApprovedAt(LocalDateTime.now());

		createSettlement(payment);

		log.info("[WEBHOOK SUCCESS] payment approved. paymentId={}", payment.getId());

		return paymentRepository.save(payment);
	}

	@Override
	@Transactional
	public Payment failPayment(Long paymentId, String reason) {

		Payment payment = paymentRepository.findById(paymentId)
			.orElseThrow(() -> new RuntimeException("Payment not found"));

		payment.setStatus(PaymentStatus.FAILED);
		payment.setReceipt(reason);

		return paymentRepository.save(payment);
	}

	private void createSettlement(Payment payment) {

		if (settlementRepository.findByPaymentId(payment.getId()).isPresent()) {
			log.info("[SETTLEMENT IDEMPOTENT] already exists. paymentId={}", payment.getId());
			return;
		}

		settlementRepository.save(
			Settlement.builder()
				.paymentId(payment.getId())
				.sellerId(payment.getSellersId())
				.status(SettlementStatus.HOLDING)
				.holdAt(LocalDateTime.now())
				.fee(payment.getFee())
				.net(payment.getNet())
				.build()
		);
	}
}
