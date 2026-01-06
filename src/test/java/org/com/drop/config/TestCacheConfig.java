package org.com.drop.config;

import java.util.List;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@TestConfiguration
@EnableCaching
@Profile("test")
public class TestCacheConfig {

	@Bean
	@Primary
	public CacheManager cacheManager() {
		SimpleCacheManager cacheManager = new SimpleCacheManager();

		cacheManager.setCaches(List.of(
			new ConcurrentMapCache("product:detail"),
			new ConcurrentMapCache("product:list"),

			new ConcurrentMapCache("homeAuctions"),
			new ConcurrentMapCache("auction:list"),
			new ConcurrentMapCache("auction:detail")
		));

		return cacheManager;
	}
}
