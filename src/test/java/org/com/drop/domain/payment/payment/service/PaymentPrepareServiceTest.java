package org.com.drop.domain.payment.payment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.com.drop.domain.payment.method.service.PaymentMethodService;
import org.com.drop.domain.payment.payment.domain.Payment;
import org.com.drop.domain.payment.payment.domain.PaymentStatus;
import org.com.drop.domain.payment.payment.dto.PaymentPrepareResponse;
import org.com.drop.domain.winner.domain.Winner;
import org.com.drop.domain.winner.repository.WinnerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentPrepareServiceTest {

	@Mock
	private PaymentService paymentService;

	@Mock
	private PaymentMethodService paymentMethodService;

	@Mock
	private WinnerRepository winnerRepository;

	@InjectMocks
	private PaymentPrepareService paymentPrepareService;

	@Test
	@DisplayName("결제 준비 - Winner 없으면 예외")
	void prepare_whenWinnerNotFound_throws() {
		// given
		when(winnerRepository.findById(55L)).thenReturn(Optional.empty());

		// when & then
		assertThrows(IllegalArgumentException.class,
			() -> paymentPrepareService.prepare(1L, 55L)
		);
	}

	@Test
	@DisplayName("결제 준비 - 결제가 이미 PAID면 autoPaid 응답")
	void prepare_whenPaymentPaid_returnsAutoPaid() {
		// given
		Winner winner = Mockito.mock(Winner.class);
		when(winner.getFinalPrice()).thenReturn(10000L);
		when(winnerRepository.findById(55L)).thenReturn(Optional.of(winner));

		Payment payment = Mockito.mock(Payment.class);
		when(payment.getId()).thenReturn(100L);
		when(payment.getStatus()).thenReturn(PaymentStatus.PAID);

		when(paymentService.createPayment(anyLong(), anyLong())).thenReturn(payment);

		// when
		PaymentPrepareResponse response = paymentPrepareService.prepare(1L, 55L);

		// then
		assertEquals(100L, response.paymentId());
		assertEquals(PaymentStatus.PAID, response.status());
		assertTrue(response.autoPaid());
		assertNull(response.toss());
	}

	@Test
	@DisplayName("결제 준비 - 결제가 PAID가 아니면 manualPay 응답")
	void prepare_whenPaymentNotPaid_returnsManualPay() {
		// given
		Winner winner = Mockito.mock(Winner.class);
		when(winner.getFinalPrice()).thenReturn(10000L);
		when(winnerRepository.findById(55L)).thenReturn(Optional.of(winner));

		Payment payment = Mockito.mock(Payment.class);
		when(payment.getId()).thenReturn(100L);
		when(payment.getStatus()).thenReturn(PaymentStatus.REQUESTED);

		when(paymentService.createPayment(anyLong(), anyLong())).thenReturn(payment);

		// when
		PaymentPrepareResponse response = paymentPrepareService.prepare(1L, 55L);

		// then
		assertEquals(100L, response.paymentId());
		assertEquals(PaymentStatus.REQUESTED, response.status());
		assertFalse(response.autoPaid());
		assertNotNull(response.toss());
		assertEquals("payment-100", response.toss().orderId());
		assertEquals(10000L, response.toss().amount());
	}
}
