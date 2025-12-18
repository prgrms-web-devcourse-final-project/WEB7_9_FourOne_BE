package org.com.drop.domain.payment.payment.event.handler;

import org.com.drop.domain.payment.payment.domain.Payment;
import org.com.drop.domain.payment.payment.domain.PaymentStatus;
import org.com.drop.domain.payment.payment.event.AutoPaymentRequestedEvent;
import org.com.drop.domain.payment.payment.event.PaymentApprovedEvent;
import org.com.drop.domain.payment.payment.event.PaymentFailedEvent;
import org.com.drop.domain.payment.payment.service.PaymentService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoPaymentRequestedHandler {

	private final PaymentService paymentService;
	private final ApplicationEventPublisher eventPublisher;

	@EventListener
	public void handle(AutoPaymentRequestedEvent event) {

		log.info("[AutoPaymentRequestedHandler] 이벤트 수신: {}", event);

		Payment payment = paymentService.attemptAutoPayment(
			event.paymentId(),
			event.billingKey()
		);
		if (payment.getStatus() == PaymentStatus.PAID) {
			log.info("자동 결제 성공 paymentId={}", payment.getId());

			eventPublisher.publishEvent(
				new PaymentApprovedEvent(
					payment.getTossPaymentKey(),
					payment.getId()
				)
			);
		} else if (payment.getStatus() == PaymentStatus.FAILED) {
			log.info(" 자동 결제 실패 paymentId={}, reason={}",
				payment.getId(), payment.getReceipt());

			eventPublisher.publishEvent(
				new PaymentFailedEvent(
					payment.getId(),
					payment.getReceipt()
				)
			);
		} else {
			log.warn("⚠자동 결제 후 예상치 못한 상태 paymentId={}, status={}",
				payment.getId(), payment.getStatus());
		}
	}
}
