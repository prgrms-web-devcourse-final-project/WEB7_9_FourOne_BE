package org.com.drop.config;

import org.mockito.Mockito;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestRedissonConfig {

	@Bean
	public RedissonClient redissonClient() {
		return Mockito.mock(RedissonClient.class);
	}
}
