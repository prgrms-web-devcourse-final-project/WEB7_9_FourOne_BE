package org.com.drop.domain.auth.store;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

	private static final String REFRESH_TOKEN_PREFIX = "refresh:";
	private final StringRedisTemplate redisTemplate;

	@Override
	public void save(String email, String refreshToken, long expirationSeconds) {
		redisTemplate.opsForValue().set(
			REFRESH_TOKEN_PREFIX + email,
			refreshToken,
			expirationSeconds,
			TimeUnit.SECONDS
		);
		log.info("Redis에 Refresh Token 저장 완료. Key: {}, TTL: {}s", REFRESH_TOKEN_PREFIX + email, expirationSeconds);
	}

	@Override
	public void delete(String email) {
		Boolean deleted = redisTemplate.delete(REFRESH_TOKEN_PREFIX + email);

		if (Boolean.TRUE.equals(deleted)) {
			log.info("Redis에서 Refresh Token 삭제 완료. Key: {}", REFRESH_TOKEN_PREFIX + email);
		} else {
			log.warn("Redis에서 Refresh Token을 찾을 수 없거나 삭제 실패. Key: {}", REFRESH_TOKEN_PREFIX + email);
		}
	}

	@Override
	public boolean exists(String email, String refreshToken) {
		String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + email);
		String key = REFRESH_TOKEN_PREFIX + email;

		if (storedToken == null) {
			log.debug("Redis에 Refresh Token 없음 (만료됨). Key: {}", key);
			return false;
		}

		boolean isMatch = storedToken.equals(refreshToken);

		if (isMatch) {
			log.debug("Redis Refresh Token 확인: 일치. Key: {}", key);
		} else {
			log.warn("Redis Refresh Token 불일치 감지 (토큰 위변조 또는 탈취 의심). Key: {}", key);
		}

		return isMatch;
	}
}
