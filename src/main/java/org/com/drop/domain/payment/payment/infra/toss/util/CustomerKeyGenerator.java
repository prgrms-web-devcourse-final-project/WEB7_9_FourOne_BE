package org.com.drop.domain.payment.payment.infra.toss.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.com.drop.domain.payment.payment.infra.toss.config.TossPaymentsProperties;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomerKeyGenerator {

	private final TossPaymentsProperties props;

	public String generate(String seed) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(
				props.customerKeyHmacSecret().getBytes(StandardCharsets.UTF_8),
				"HmacSHA256"
			));

			byte[] rawHmac = mac.doFinal(seed.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);

		} catch (Exception e) {
			throw new IllegalStateException("Failed to generate customerKey", e);
		}
	}
}
