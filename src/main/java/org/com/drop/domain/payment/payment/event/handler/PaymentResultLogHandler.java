package org.com.drop.domain.payment.payment.event.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.drop.domain.payment.payment.event.PaymentApprovedEvent;
import org.com.drop.domain.payment.payment.event.PaymentFailedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentResultLogHandler {

	@EventListener
	public void handleApproved(PaymentApprovedEvent event) {
		log.info("[PaymentResult] 결제 승인 완료 paymentId={}, paymentKey={}",
			event.paymentId(), event.paymentKey());
	}

	@EventListener
	public void handleFailed(PaymentFailedEvent event) {
		log.info("[PaymentResult] 결제 실패 paymentId={}, reason={}",
			event.paymentId(), event.reason());
	}
}
