package org.com.drop.domain.payment.payment.event.handler;

import org.com.drop.domain.payment.payment.event.AutoPaymentRequestedEvent;
import org.com.drop.domain.payment.payment.event.PaymentRequestedEvent;
import org.com.drop.domain.payment.payment.service.PaymentService;
import org.com.drop.domain.payment.payment.service.UserPaymentMethodService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentPrepareHandler {

	private final PaymentService paymentService;
	private final UserPaymentMethodService userPaymentMethodService;
	private final ApplicationEventPublisher eventPublisher;

	@EventListener
	public void handle(PaymentRequestedEvent event) {

		log.info("[PaymentPrepareHandler] PaymentRequestedEvent 수신: {}", event);
		var payment = paymentService.createPayment(
			event.winnerId(),
			event.amount()
		);

		log.info("➡ Payment REQUESTED 생성 완료. paymentId={}", payment.getId());
		var userPaymentInfo = userPaymentMethodService.getUserPaymentInfo(event.userId());

		if (userPaymentInfo.autoPayEnabled()) {
			eventPublisher.publishEvent(
				new AutoPaymentRequestedEvent(
					payment.getId(),
					event.userId(),
					userPaymentInfo.billingKey(),
					event.amount()
				)
			);
		} else {
			log.info("자동결제 OFF - 사용자가 직접 결제해야 함");
		}
	}
}

