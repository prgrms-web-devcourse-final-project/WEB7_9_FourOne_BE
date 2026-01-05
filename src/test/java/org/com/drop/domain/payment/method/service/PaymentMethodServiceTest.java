package org.com.drop.domain.payment.method.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.com.drop.domain.payment.method.dto.RegisterCardRequest;
import org.com.drop.domain.payment.method.entity.PaymentMethod;
import org.com.drop.domain.payment.method.repository.PaymentMethodRepository;
import org.com.drop.domain.payment.payment.domain.CardCompany;
import org.com.drop.domain.payment.payment.infra.toss.util.CustomerKeyGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentMethodServiceTest {

	@Mock
	private PaymentMethodRepository paymentMethodRepository;

	@Mock
	private CustomerKeyGenerator customerKeyGenerator;

	@InjectMocks
	private PaymentMethodService paymentMethodService;

	@Test
	@DisplayName("카드 등록 - billingKey 중복이면 예외 + save 안 함")
	void registerCard_whenBillingKeyExists_throws() {
		// given
		RegisterCardRequest request = new RegisterCardRequest(
			"bill_123",
			CardCompany.SAMSUNG,
			"1234-****-****-5678",
			"MyCard"
		);

		when(paymentMethodRepository.existsByBillingKey("bill_123")).thenReturn(true);

		// when & then
		assertThrows(RuntimeException.class,
			() -> paymentMethodService.registerCard(1L, request)
		);

		verify(paymentMethodRepository, never()).save(any());
	}

	@Test
	@DisplayName("카드 등록 - customerKey 생성 후 저장")
	void registerCard_savesPaymentMethod() {
		// given
		RegisterCardRequest request = new RegisterCardRequest(
			"bill_1234",
			CardCompany.SAMSUNG,
			"1234-****-****-5679",
			"MyCard"
		);

		when(paymentMethodRepository.existsByBillingKey("bill_1234")).thenReturn(false);
		when(customerKeyGenerator.generate("user:1bill_1234")).thenReturn("customer_key");

		ArgumentCaptor<PaymentMethod> captor = ArgumentCaptor.forClass(PaymentMethod.class);

		// when
		paymentMethodService.registerCard(1L, request);

		// then
		verify(paymentMethodRepository, times(1)).save(captor.capture());
		PaymentMethod saved = captor.getValue();

		assertEquals(1L, saved.getUserId());
		assertEquals(CardCompany.SAMSUNG, saved.getCardCompany());
		assertEquals("customer_key", saved.getCustomerKey());
		assertEquals("bill_1234", saved.getBillingKey());
		assertEquals("1234-****-****-5679", saved.getCardNumberMasked());
		assertEquals("MyCard", saved.getCardName());
	}

	@Test
	@DisplayName("카드 삭제 - cardId 없으면 예외 + delete 안 함")
	void deleteCard_whenNotFound_throws() {
		// given
		when(paymentMethodRepository.findById(10L)).thenReturn(Optional.empty());

		// when & then
		assertThrows(RuntimeException.class,
			() -> paymentMethodService.deleteCard(1L, 10L)
		);

		verify(paymentMethodRepository, never()).delete(any());
	}

	@Test
	@DisplayName("카드 삭제 - 소유자 불일치면 예외 + delete 안 함")
	void deleteCard_whenUserMismatch_throws() {
		// given
		PaymentMethod method = new PaymentMethod(
			2L,
			CardCompany.SAMSUNG,
			"ck",
			"bk",
			"mask",
			"name"
		);

		when(paymentMethodRepository.findById(10L)).thenReturn(Optional.of(method));

		// when & then
		assertThrows(RuntimeException.class,
			() -> paymentMethodService.deleteCard(1L, 10L)
		);

		verify(paymentMethodRepository, never()).delete(any());
	}
}
