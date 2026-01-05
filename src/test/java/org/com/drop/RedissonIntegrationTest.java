package org.com.drop;

import org.com.drop.config.TestRedissonConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestRedissonConfig.class)
@ActiveProfiles("test")
public abstract class RedissonIntegrationTest {
}
