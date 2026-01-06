package org.com.drop.domain.payment.payment.event.handler;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.com.drop.domain.payment.method.entity.PaymentMethod;
import org.com.drop.domain.payment.method.service.PaymentMethodService;
import org.com.drop.domain.payment.payment.domain.Payment;
import org.com.drop.domain.payment.payment.event.AutoPaymentRequestedEvent;
import org.com.drop.domain.payment.payment.event.PaymentRequestedEvent;
import org.com.drop.domain.payment.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PaymentPrepareHandlerTest {

	@Mock
	PaymentService paymentService;

	@Mock
	PaymentMethodService paymentMethodService;

	@Mock
	ApplicationEventPublisher eventPublisher;

	@InjectMocks
	PaymentPrepareHandler handler;

	@Test
	void handle_whenCardExists_publishAutoPaymentRequestedEvent_withCorrectPayload() {
		// given
		Payment payment = mock(Payment.class);
		when(payment.getId()).thenReturn(1L);

		PaymentMethod method = mock(PaymentMethod.class);
		when(method.getBillingKey()).thenReturn("billing-key");

		when(paymentService.createPayment(eq(1L), eq(1000L))).thenReturn(payment);
		when(paymentMethodService.getPrimaryMethod(eq(2L))).thenReturn(method);

		// when
		handler.handle(new PaymentRequestedEvent(1L, 2L, 1000L));

		// then: 호출 인자 검증
		verify(paymentService).createPayment(1L, 1000L);
		verify(paymentMethodService).getPrimaryMethod(2L);

		// then: 이벤트 payload 검증
		ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
		verify(eventPublisher).publishEvent(captor.capture());

		assertThat(captor.getValue()).isInstanceOf(AutoPaymentRequestedEvent.class);
		AutoPaymentRequestedEvent ev = (AutoPaymentRequestedEvent)captor.getValue();

		assertThat(ev.paymentId()).isEqualTo(1L);
		assertThat(ev.userId()).isEqualTo(2L);
		assertThat(ev.billingKey()).isEqualTo("billing-key");
		assertThat(ev.amount()).isEqualTo(1000L);
	}

	@Test
	void handle_whenNoCard_doNotPublishAutoPaymentEvent() {
		// given
		Payment payment = mock(Payment.class);
		when(payment.getId()).thenReturn(1L);

		when(paymentService.createPayment(eq(1L), eq(1000L))).thenReturn(payment);
		when(paymentMethodService.getPrimaryMethod(eq(2L)))
			.thenThrow(new RuntimeException("no card"));

		// when
		handler.handle(new PaymentRequestedEvent(1L, 2L, 1000L));

		// then
		verify(eventPublisher, never()).publishEvent(any());
	}
}

