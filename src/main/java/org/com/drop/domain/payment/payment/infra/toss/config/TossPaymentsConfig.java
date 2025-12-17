package org.com.drop.domain.payment.payment.infra.toss.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(TossPaymentsProperties.class)
public class TossPaymentsConfig {

	private final TossPaymentsProperties props;

	@Bean
	public RestClient tossRestClient() {
		String basicRaw = props.secretKey() + ":";
		String basic = Base64.getEncoder().encodeToString(basicRaw.getBytes(StandardCharsets.UTF_8));

		return RestClient.builder()
			.baseUrl(props.baseUrl())
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basic)
			.build();
	}
}
