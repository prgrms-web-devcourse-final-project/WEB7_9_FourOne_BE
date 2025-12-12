package org.com.drop.domain.auth.service;


public interface RefreshTokenStore {
	void save(String email, String refreshToken, long expirationSeconds);
	void delete(String email);
}
