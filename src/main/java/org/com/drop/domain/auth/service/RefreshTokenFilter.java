package org.com.drop.domain.auth.service;

import java.io.IOException;

import org.com.drop.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class RefreshTokenFilter extends OncePerRequestFilter {

	private final ObjectMapper objectMapper;

	@Override
	protected void doFilterInternal(HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain)
		throws ServletException, IOException {

		if (!request.getRequestURI().equals("/api/v1/auth/refresh")) {
			filterChain.doFilter(request, response);
			return;
		}

		String refreshToken = extractRefreshTokenFromCookie(request);

		if (refreshToken == null || refreshToken.isBlank()) {

			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType("application/json;charset=UTF-8");

			String jsonError = objectMapper.writeValueAsString(
				new ErrorResponse(
					ErrorCode.AUTH_TOKEN_MISSING.getCode(),
					ErrorCode.AUTH_TOKEN_MISSING.getMessage() + ": refreshToken missing or empty"
				)
			);
			response.getWriter().write(jsonError);
			return;
		}

		request.setAttribute("validRefreshToken", refreshToken);

		filterChain.doFilter(request, response);
	}

	private String extractRefreshTokenFromCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) return null;

		for (Cookie cookie : cookies) {
			if ("refreshToken".equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}

	private record ErrorResponse(String code, String message) {}
}
