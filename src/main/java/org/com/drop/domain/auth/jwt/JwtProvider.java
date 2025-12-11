package org.com.drop.domain.auth.jwt;

import java.util.Date;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtProvider {

	private final JwtProperties jwtProperties;

	public String createAccessToken(String username) {
		Date now = new Date();
		Date expire = new Date(now.getTime() + jwtProperties.getAccessTokenExpire() * 1000);

		return Jwts.builder()
			.setSubject(username)
			.setIssuedAt(now)
			.setExpiration(expire)
			.signWith(SignatureAlgorithm.HS256, jwtProperties.getSecret())
			.compact();
	}

	public String createRefreshToken(String username) {
		Date now = new Date();
		Date expire = new Date(now.getTime() + jwtProperties.getRefreshTokenExpire() * 1000);

		return Jwts.builder()
			.setSubject(username)
			.setIssuedAt(now)
			.setExpiration(expire)
			.signWith(SignatureAlgorithm.HS256, jwtProperties.getSecret())
			.compact();
	}

	public String getUsername(String token) {
		return Jwts.parser()
			.setSigningKey(jwtProperties.getSecret())
			.parseClaimsJws(token)
			.getBody()
			.getSubject();
	}

	public long getAccessTokenValidityInSeconds() {
		return jwtProperties.getAccessTokenExpire();
	}
}
