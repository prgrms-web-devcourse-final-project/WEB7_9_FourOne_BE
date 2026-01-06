package org.com.drop.domain.auth.store;

import java.util.concurrent.TimeUnit;

import org.com.drop.global.exception.ErrorCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationCodeStore {
	private static final String CODE_PREFIX = "email_code:";
	private static final long CODE_TTL_MINUTES = 5;
	private static final String VERIFIED_EMAIL_PREFIX = "verified:";
	private static final long VERIFIED_TTL_MINUTES = 30;
	private final StringRedisTemplate redisTemplate;

	@CircuitBreaker(name = "redisService", fallbackMethod = "fallbackRedisError")
	public void saveCode(String email, String code) {
		String key = CODE_PREFIX + email;
		redisTemplate.opsForValue().set(key, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);
		log.debug("Redis에 인증 코드 저장 완료. Key: {}, TTL: {}분", key, CODE_TTL_MINUTES);
	}

	@CircuitBreaker(name = "redisService", fallbackMethod = "fallbackRedisGetError")
	public String getCode(String email) {
		return redisTemplate.opsForValue().get(CODE_PREFIX + email);
	}

	@CircuitBreaker(name = "redisService", fallbackMethod = "fallbackRedisError")
	public void removeCode(String email) {
		redisTemplate.delete(CODE_PREFIX + email);
	}

	public void markAsVerified(String email) {
		redisTemplate.opsForValue().set(
			VERIFIED_EMAIL_PREFIX + email,
			"true",
			VERIFIED_TTL_MINUTES,
			TimeUnit.MINUTES
		);
	}

	public boolean isVerified(String email) {
		return redisTemplate.hasKey(VERIFIED_EMAIL_PREFIX + email);
	}

	public void removeVerifiedMark(String email) {
		redisTemplate.delete(VERIFIED_EMAIL_PREFIX + email);
	}

	private void fallbackRedisError(String email, String value, Throwable t) {
		log.error("VerificationCodeStore Redis 장애 (Void): {}", t.getMessage());
		throw ErrorCode.SYSTEM_ERROR.serviceException("현재 인증 시스템을 이용할 수 없습니다.");
	}

	// getCode 처럼 리턴 타입이 String인 메서드용
	private String fallbackRedisGetError(String email, Throwable t) {
		log.error("VerificationCodeStore Redis 장애 (Get): {}", t.getMessage());
		throw ErrorCode.SYSTEM_ERROR.serviceException("인증 확인 중 오류가 발생했습니다.");
	}
}
