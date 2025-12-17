package org.com.drop.domain.payment.payment.infra.toss.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toss.payments")
public record TossPaymentsProperties(
	String baseUrl,
	String secretKey,
	String customerKeyHmacSecret
) { }
