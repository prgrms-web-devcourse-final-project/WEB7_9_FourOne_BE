package org.com.drop.domain.auth.jwt;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

	private final JwtProvider jwtProvider;
	private final UserDetailsService userDetailsService;

	@Override
	protected void doFilterInternal(HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain)
		throws ServletException, IOException {

		String token = resolveToken(request);

		if (token != null) {
			try {
				String username = jwtProvider.getUsername(token);
				UserDetails userDetails = userDetailsService.loadUserByUsername(username);

				UsernamePasswordAuthenticationToken auth =
					new UsernamePasswordAuthenticationToken(
						userDetails, null, userDetails.getAuthorities()
					);
				SecurityContextHolder.getContext().setAuthentication(auth);

			} catch (Exception e) {
				// 토큰 invalid → 그냥 다음 필터로 넘김(401은 컨트롤러 단에서)
			}
		}

		filterChain.doFilter(request, response);
	}

	private String resolveToken(HttpServletRequest request) {
		String header = request.getHeader("Authorization");

		if (header != null && header.startsWith("Bearer ")) {
			return header.substring(7);
		}

		return null;
	}
}
