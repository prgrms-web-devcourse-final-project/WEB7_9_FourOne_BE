package org.com.drop.global.config;

import java.util.List;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class CacheConfig {

	@Bean
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
