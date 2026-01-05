package org.com.drop.domain.payment.payment.event.handler;

import org.com.drop.domain.payment.method.entity.PaymentMethod;
import org.com.drop.domain.payment.method.service.PaymentMethodService;
import org.com.drop.domain.payment.payment.event.AutoPaymentRequestedEvent;
import org.com.drop.domain.payment.payment.event.PaymentRequestedEvent;
import org.com.drop.domain.payment.payment.service.PaymentService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentPrepareHandler {

	private final PaymentService paymentService;
	private final PaymentMethodService paymentMethodService;
	private final ApplicationEventPublisher eventPublisher;

	@EventListener
	public void handle(PaymentRequestedEvent event) {

		log.info("[PaymentPrepareHandler] PaymentRequestedEvent 수신: {}", event);

		var payment = paymentService.createPayment(
			event.winnerId(),
			event.amount()
		);

		log.info("➡ Payment REQUESTED 생성 완료. paymentId={}", payment.getId());

		try {
			PaymentMethod method =
				paymentMethodService.getPrimaryMethod(event.userId());

			eventPublisher.publishEvent(
				new AutoPaymentRequestedEvent(
					payment.getId(),
					event.userId(),
					method.getBillingKey(),
					event.amount()
				)
			);

		} catch (Exception e) {
			log.info("자동결제 불가 - 등록된 카드 없음 userId={}", event.userId());
			// 자동결제 불가 → 프론트 결제 대기 상태
		}
	}
}

