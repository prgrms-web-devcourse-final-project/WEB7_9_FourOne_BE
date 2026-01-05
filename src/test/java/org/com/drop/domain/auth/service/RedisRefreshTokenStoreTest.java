package org.com.drop.domain.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.com.drop.domain.auth.store.RedisRefreshTokenStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;

import jakarta.transaction.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class RedisRefreshTokenStoreTest {
	private static final String EMAIL = "test@example.com";
	private static final String TOKEN = "refresh-token";
	private static final long TTL = 3600L;
	private static final String KEY = "refresh:" + EMAIL;
	@Mock
	StringRedisTemplate redisTemplate;
	@Mock
	ValueOperations<String, String> valueOperations;
	@InjectMocks
	private RedisRefreshTokenStore redisRefreshTokenStore;

	@Nested
	class SaveTest {
		@Test
		@DisplayName("저장-성공")
		void t1() throws Exception {
			when(redisTemplate.opsForValue()).thenReturn(valueOperations);

			redisRefreshTokenStore.save(EMAIL, TOKEN, TTL);

			verify(valueOperations, times(1))
				.set(
					"refresh:" + EMAIL,
					TOKEN,
					TTL,
					TimeUnit.SECONDS
				);
		}
	}

	@Nested
	class DeleteTest {
		@Test
		@DisplayName("삭제-성공")
		void t2() {
			when(redisTemplate.delete(KEY)).thenReturn(true);

			redisRefreshTokenStore.delete(EMAIL);

			verify(redisTemplate).delete(KEY);
		}

		@Test
		@DisplayName("삭제-실패-키 없음")
		void t2_1() {
			when(redisTemplate.delete(KEY)).thenReturn(false);

			redisRefreshTokenStore.delete(EMAIL);

			verify(redisTemplate).delete(KEY);
		}
	}

	@Nested
	class ExistsTest {
		private static final String STORED_TOKEN = "stored-token";
		private static final String INPUT_TOKEN = "stored-token";
		private static final String WRONG_TOKEN = "wrong-token";

		@BeforeEach
		void setUp() {
			when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		}

		@Test
		@DisplayName("조회-없음-성공")
		void t3() {
			when(valueOperations.get(KEY)).thenReturn(null);

			boolean result = redisRefreshTokenStore.exists(EMAIL, INPUT_TOKEN);

			assertThat(result).isFalse();
			verify(valueOperations).get(KEY);
		}

		@Test
		@DisplayName("조회-없음-실패(토큰 불일치)")
		void t3_1() {
			when(valueOperations.get(KEY)).thenReturn(STORED_TOKEN);

			boolean result = redisRefreshTokenStore.exists(EMAIL, WRONG_TOKEN);

			assertThat(result).isFalse();
			verify(valueOperations).get(KEY);
		}


		@Test
		@DisplayName("조회-성공-있음")
		void t4() {
			when(valueOperations.get(KEY)).thenReturn(STORED_TOKEN);

			boolean result = redisRefreshTokenStore.exists(EMAIL, STORED_TOKEN);

			assertThat(result).isTrue();
			verify(valueOperations).get(KEY);
		}


	}
}
