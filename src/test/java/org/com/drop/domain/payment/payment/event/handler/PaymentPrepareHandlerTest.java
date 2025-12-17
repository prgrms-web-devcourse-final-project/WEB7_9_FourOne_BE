package org.com.drop.domain.payment.payment.event.handler;

import org.com.drop.domain.payment.payment.domain.Payment;
import org.com.drop.domain.payment.payment.event.AutoPaymentRequestedEvent;
import org.com.drop.domain.payment.payment.event.PaymentRequestedEvent;
import org.com.drop.domain.payment.payment.service.PaymentService;
import org.com.drop.domain.payment.payment.service.UserPaymentMethodService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentPrepareHandlerTest {

	@Mock
	PaymentService paymentService;

	@Mock
	UserPaymentMethodService userPaymentMethodService;

	@Mock
	ApplicationEventPublisher eventPublisher;

	@InjectMocks
	PaymentPrepareHandler handler;

	@Test
	void handle_whenAutoPayEnabled_publishAutoPaymentRequestedEvent() {
		Payment payment = Payment.builder().id(1L).build();

		when(paymentService.createPayment(any(), any()))
			.thenReturn(payment);

		when(userPaymentMethodService.getUserPaymentInfo(2L))
			.thenReturn(new UserPaymentMethodService.UserPaymentInfo(true, "billing-key"));

		handler.handle(new PaymentRequestedEvent(1L, 2L, 1000L));

		verify(eventPublisher)
			.publishEvent(any(AutoPaymentRequestedEvent.class));
	}

	@Test
	void handle_whenAutoPayDisabled_doNotPublishAutoPaymentEvent() {
		Payment payment = Payment.builder().id(1L).build();

		when(paymentService.createPayment(any(), any()))
			.thenReturn(payment);

		when(userPaymentMethodService.getUserPaymentInfo(2L))
			.thenReturn(new UserPaymentMethodService.UserPaymentInfo(false, null));

		handler.handle(new PaymentRequestedEvent(1L, 2L, 1000L));

		verify(eventPublisher, never())
			.publishEvent(any(AutoPaymentRequestedEvent.class));
	}
}


