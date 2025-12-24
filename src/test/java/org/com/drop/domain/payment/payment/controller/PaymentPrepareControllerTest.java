package org.com.drop.domain.payment.payment.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.com.drop.domain.payment.payment.dto.PaymentPrepareRequest;
import org.com.drop.domain.payment.payment.dto.PaymentPrepareResponse;
import org.com.drop.domain.payment.payment.service.PaymentPrepareService;
import org.com.drop.domain.user.entity.User;
import org.com.drop.global.rsdata.RsData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PaymentPrepareControllerTest {

	@Test
	@DisplayName("결제 준비 - 자동결제 성공")
	void prepare_autoPaid_returnsAutoPaidResponse() {
		// given
		PaymentPrepareService paymentPrepareService = Mockito.mock(PaymentPrepareService.class);
		PaymentPrepareController controller = new PaymentPrepareController(paymentPrepareService);

		User user = Mockito.mock(User.class);
		when(user.getId()).thenReturn(1L);

		PaymentPrepareRequest request = new PaymentPrepareRequest(10L);
		PaymentPrepareResponse response = PaymentPrepareResponse.autoPaid(100L);

		when(paymentPrepareService.prepare(1L, 10L)).thenReturn(response);

		// when
		RsData<PaymentPrepareResponse> result = controller.prepare(user, request);

		// then
		assertThat(result.getData().paymentId()).isEqualTo(100L);
		assertThat(result.getData().autoPaid()).isTrue();
	}
}

