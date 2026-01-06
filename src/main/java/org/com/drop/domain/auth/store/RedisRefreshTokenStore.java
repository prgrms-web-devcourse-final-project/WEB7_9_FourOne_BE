package org.com.drop.domain.auth.store;

import java.util.concurrent.TimeUnit;

import org.com.drop.global.exception.ErrorCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

	private static final String REFRESH_TOKEN_PREFIX = "refresh:";
	private final StringRedisTemplate redisTemplate;

	@Override
	@CircuitBreaker(name = "redisService", fallbackMethod = "fallbackSave")
	public void save(String email, String refreshToken, long expirationSeconds) {
		redisTemplate.opsForValue().set(
			REFRESH_TOKEN_PREFIX + email,
			refreshToken,
			expirationSeconds,
			TimeUnit.SECONDS
		);
	}

	@Override
	public void delete(String email) {
		Boolean deleted = redisTemplate.delete(REFRESH_TOKEN_PREFIX + email);

		if (Boolean.TRUE.equals(deleted)) {
			log.debug("Redis에서 Refresh Token 삭제 완료. Key: {}", REFRESH_TOKEN_PREFIX + email);
		} else {
			log.warn("Redis에서 Refresh Token을 찾을 수 없거나 삭제 실패. Key: {}", REFRESH_TOKEN_PREFIX + email);
		}
	}

	@Override
	@CircuitBreaker(name = "redisService", fallbackMethod = "fallbackExists")
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

	private void fallbackSave(String email, String refreshToken, long expirationSeconds, Throwable throwable) {
		log.error("Redis Circuit Open! 토큰 저장 실패: {}", throwable.getMessage());
		throw ErrorCode.SYSTEM_ERROR.serviceException("시스템 오류로 로그인이 불가능합니다.");
	}

	private boolean fallbackExists(String email, String refreshToken, Throwable throwable) {
		log.error("Redis Circuit Open (exists)! 원인: {}", throwable.getMessage());
		throw ErrorCode.SYSTEM_ERROR.serviceException("인증 정보 확인 중 오류가 발생했습니다.");
	}
}
