package org.com.drop;

import org.com.drop.config.TestCacheConfig;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@Import(TestCacheConfig.class)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

	@MockitoBean
	protected RedissonClient redissonClient;
}
