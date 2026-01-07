package org.com.drop.domain.auth.jwt;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JwtProvider 단위 테스트")
class JwtProviderTest {

	private JwtProvider jwtProvider;
	private JwtProperties jwtProperties;

	@BeforeEach
	void setUp() {
		jwtProperties = new JwtProperties();

		// ✅ 반드시 Base64 인코딩된 secret 사용
		jwtProperties.setSecret(
			Base64.getEncoder().encodeToString(
				"test-secret-key-test-secret-key-test-secret-key".getBytes()
			)
		);

		jwtProperties.setAccessTokenExpire(3600L);     // 1시간
		jwtProperties.setRefreshTokenExpire(1209600L); // 14일

		jwtProvider = new JwtProvider(jwtProperties);
	}

	@Test
	@DisplayName("Access Token 생성 성공")
	void createAccessToken_success() {
		// when
		String token = jwtProvider.createAccessToken("test@drop.com");

		// then
		assertNotNull(token);
		assertFalse(token.isBlank());
	}

	@Test
	@DisplayName("Refresh Token 생성 성공")
	void createRefreshToken_success() {
		// when
		String token = jwtProvider.createRefreshToken("test@drop.com");

		// then
		assertNotNull(token);
		assertFalse(token.isBlank());
	}

	@Test
	@DisplayName("Access Token에서 username(subject) 추출 성공")
	void getUsername_success() {
		// given
		String token = jwtProvider.createAccessToken("test@drop.com");

		// when
		String username = jwtProvider.getUsername(token);

		// then
		assertEquals("test@drop.com", username);
	}

	@Test
	@DisplayName("잘못된 토큰이면 예외 발생")
	void getUsername_fail_invalidToken() {
		// given
		String invalidToken = "invalid.jwt.token";

		// when & then
		assertThrows(Exception.class, () ->
			jwtProvider.getUsername(invalidToken)
		);
	}

	@Test
	@DisplayName("Access Token 유효시간 반환")
	void getAccessTokenValidityInSeconds_success() {
		// when
		long expire = jwtProvider.getAccessTokenValidityInSeconds();

		// then
		assertEquals(3600L, expire);
	}
}
