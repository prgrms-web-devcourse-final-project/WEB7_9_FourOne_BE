package org.com.drop.domain.payment.payment.infra.toss.webhook;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.com.drop.domain.auth.jwt.JwtProvider;
import org.com.drop.domain.payment.payment.service.PaymentService;
import org.com.drop.domain.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TossWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
class TossWebhookControllerTest {

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private PaymentService paymentService;

	@MockitoBean
	private JwtProvider jwtProvider;

	@Autowired
	private MockMvc mockMvc;

	private static final String WEBHOOK_BODY = """
		{
			"eventType": "PAYMENT_STATUS_CHANGED",
			"data":	{
			"paymentKey": "pk_test",
			"orderId":	"payment-10",
			"status":	"DONE",
			"totalAmount": 1000
			}
		}""";

	@Test
	void webhook_success_callsConfirmPayment() throws Exception {

		mockMvc.perform(
			post("/webhooks/toss")
				.contentType(MediaType.APPLICATION_JSON)
				.content(WEBHOOK_BODY)
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
				.content(WEBHOOK_BODY)
		).andExpect(status().isOk());
	}
}
