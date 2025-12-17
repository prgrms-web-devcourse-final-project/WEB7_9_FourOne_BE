package org.com.drop.domain.payment.payment.infra.toss;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.drop.domain.payment.payment.infra.toss.dto.TossAutoPayRequest;
import org.com.drop.domain.payment.payment.infra.toss.dto.TossAutoPayResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class TossPaymentsClientImpl implements TossPaymentsClient {

	private final RestClient tossRestClient;

	@Override
	public TossAutoPayResponse approveBilling(String billingKey, TossAutoPayRequest request, String idempotencyKey) {
		return tossRestClient.post()
			.uri("/v1/billing/{billingKey}", billingKey)
			.contentType(MediaType.APPLICATION_JSON)
			.header("Idempotency-Key", idempotencyKey)
			.body(request)
			.retrieve()
			.body(TossAutoPayResponse.class);
	}
}
