package org.com.drop.domain.payment.payment.event.handler;

import org.com.drop.domain.payment.payment.domain.Payment;
import org.com.drop.domain.payment.payment.domain.PaymentStatus;
import org.com.drop.domain.payment.payment.event.AutoPaymentRequestedEvent;
import org.com.drop.domain.payment.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoPaymentRequestedHandlerTest {

	@Mock
	PaymentService paymentService;

	@Mock
	ApplicationEventPublisher eventPublisher;

	@InjectMocks
	AutoPaymentRequestedHandler handler;

	@Test
	void handle_callsAttemptAutoPayment_whenPaymentIsRequested() {
		// given
		Payment payment = Payment.builder()
			.id(1L)
			.status(PaymentStatus.PAID)
			.tossPaymentKey("toss-key")
			.build();

		when(paymentService.attemptAutoPayment(1L, "billing-key"))
			.thenReturn(payment);

		AutoPaymentRequestedEvent event =
			new AutoPaymentRequestedEvent(1L, 2L, "billing-key", 1000L);

		// when
		handler.handle(event);

		// then
		verify(paymentService)
			.attemptAutoPayment(1L, "billing-key");
	}
}


