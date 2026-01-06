package org.com.drop;

import org.com.drop.config.TestCacheConfig;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestCacheConfig.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
	"spring.main.allow-bean-definition-overriding=true"
})
public abstract class BaseIntegrationTest {

	@MockitoBean
	protected RedissonClient redissonClient;
}
