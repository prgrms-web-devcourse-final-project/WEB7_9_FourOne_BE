package org.com.drop.domain.auth.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

// Redis 적용 전 더미 파일(임시)
@Component
public class RefreshTokenStore {
	private static final Map<String, String> store = new ConcurrentHashMap<>();

	public static void delete(String userEmail) {
		store.remove(userEmail);
	}

	public void save(String userEmail, String refreshToken) {
		store.put(userEmail, refreshToken);
	}

	public String find(String userEmail) {
		return store.get(userEmail);
	}
}
