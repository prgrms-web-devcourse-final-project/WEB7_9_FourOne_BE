package org.com.drop.domain.auth.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

	@Override
	public void save(String email, String refreshToken, long expirationSeconds) {
		System.out.println("Redis에 Refresh Token 저장");
	}

	@Override
	public void delete(String email) {
		System.out.println("Redis에서 Refresh Token 삭제");
	}

	@Override
	public boolean exists(String email, String refreshToken) {
		System.out.println("Redis에 Refresh Token 확인");
		return false;
	}
}
