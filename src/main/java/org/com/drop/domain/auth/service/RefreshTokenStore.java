package org.com.drop.domain.auth.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

// Redis 적용을 위한 더미 파일(임시)
@Component
public class RefreshTokenStore {
	private final Map<String, String> store = new ConcurrentHashMap<>();

	public void save(String username, String refreshToken) {
		store.put(username, refreshToken);
	}

	public String find(String username) {
		return store.get(username);
	}

	public void delete(String username) {
		store.remove(username);
	}
}
