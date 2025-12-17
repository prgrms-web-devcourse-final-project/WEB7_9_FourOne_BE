package org.com.drop.domain.payment.payment.infra.toss.webhook;

import org.com.drop.domain.auth.jwt.JwtProvider;
import org.com.drop.domain.payment.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TossWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
class TossWebhookControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private PaymentService paymentService;

	/**
	 * JwtFilter가 의존하는 Bean이므로 "존재만" 시켜준다
	 * 실제 동작은 전혀 테스트하지 않음
	 */
	@MockBean
	private JwtProvider jwtProvider;

	@Test
	void webhook_success_callsConfirmPayment() throws Exception {

		mockMvc.perform(
			post("/webhooks/toss")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "eventType": "PAYMENT_STATUS_CHANGED",
					  "data": {
					    "paymentKey": "pk_test",
					    "orderId": "payment-10",
					    "status": "DONE",
					    "totalAmount": 1000
					  }
					}
				""")
		).andExpect(status().isOk());

		verify(paymentService).confirmPaymentByWebhook(
			"pk_test",
			10L,
			1000L
		);
	}

	@Test
	void webhook_idempotent_returnsOk() throws Exception {

		doThrow(new RuntimeException())
			.when(paymentService)
			.confirmPaymentByWebhook(any(), any(), any());

		mockMvc.perform(
			post("/webhooks/toss")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "eventType": "PAYMENT_STATUS_CHANGED",
					  "data": {
					    "paymentKey": "pk_test",
					    "orderId": "payment-10",
					    "status": "DONE",
					    "totalAmount": 1000
					  }
					}
				""")
		).andExpect(status().isOk());
	}
}

