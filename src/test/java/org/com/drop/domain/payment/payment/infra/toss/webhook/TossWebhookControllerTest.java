package org.com.drop.domain.payment.payment.infra.toss.webhook;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.com.drop.domain.auth.jwt.JwtProvider;
import org.com.drop.domain.payment.payment.service.PaymentService;
import org.com.drop.global.security.auth.LoginUserArgumentResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
	controllers = TossWebhookController.class,
	excludeAutoConfiguration = {
		SecurityAutoConfiguration.class,
		SecurityFilterAutoConfiguration.class
	}
)
@AutoConfigureMockMvc(addFilters = false)
class TossWebhookControllerTest {

	@MockitoBean
	private LoginUserArgumentResolver loginUserArgumentResolver;

	@MockitoBean
	private UserDetailsService userDetailsService;

	@MockitoBean
	private JwtProvider jwtProvider;

	@MockitoBean
	private PaymentService paymentService;

	@Autowired
	private MockMvc mockMvc;

	private static final String DONE_BODY = """
		{
			"eventType": "PAYMENT_STATUS_CHANGED",
			"data": {
				"paymentKey": "pk_test",
				"orderId": "payment-10",
				"status": "DONE",
				"totalAmount": 1000
			}
		}
		""";

	@Test
	void webhook_success_callsConfirmPayment() throws Exception {
		mockMvc.perform(
			post("/webhooks/toss")
				.contentType(MediaType.APPLICATION_JSON)
				.content(DONE_BODY)
		).andExpect(status().isOk());

		verify(paymentService).confirmPaymentByWebhook("pk_test", 10L, 1000L);
	}

	@Test
	void webhook_whenEventTypeNotMatched_shouldIgnore() throws Exception {
		String body = """
			{
				"eventType": "SOMETHING_ELSE",
				"data": {
					"paymentKey": "pk_test",
					"orderId": "payment-10",
					"status": "DONE",
					"totalAmount": 1000
				}
			}
			""";

		mockMvc.perform(post("/webhooks/toss")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk());

		verify(paymentService, never()).confirmPaymentByWebhook(any(), any(), any());
	}

	@Test
	void webhook_whenStatusNotDone_shouldIgnore() throws Exception {
		String body = """
			{
				"eventType": "PAYMENT_STATUS_CHANGED",
				"data": {
					"paymentKey": "pk_test",
					"orderId": "payment-10",
					"status": "CANCELED",
					"totalAmount": 1000
				}
			}
			""";

		mockMvc.perform(post("/webhooks/toss")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isOk());

		verify(paymentService, never()).confirmPaymentByWebhook(any(), any(), any());
	}

	@Test
	void webhook_whenServiceThrows_shouldStillReturnOk() throws Exception {
		doThrow(new RuntimeException("boom"))
			.when(paymentService)
			.confirmPaymentByWebhook(any(), any(), any());

		mockMvc.perform(post("/webhooks/toss")
				.contentType(MediaType.APPLICATION_JSON)
				.content(DONE_BODY))
			.andExpect(status().isOk());
	}
}
