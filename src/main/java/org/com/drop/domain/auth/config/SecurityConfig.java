package org.com.drop.domain.auth.config;

import org.com.drop.domain.auth.jwt.JwtFilter;
import org.com.drop.domain.auth.service.RefreshTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtFilter jwtFilter;
	private final RefreshTokenFilter refreshTokenFilter;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			.cors(Customizer.withDefaults())
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
				.requestMatchers("/error").permitAll()
				.requestMatchers(
					"/api/v1/auth/login",
					"/api/v1/auth/local/signup",
					"/api/v1/auth/email/verify-code",
					"/api/v1/auth/email/send-code",
					"/api/v1/auth/refresh",
					"/api/v1/admin/help/**"
				).permitAll()
				.requestMatchers("/api/v1/auth/logout").authenticated()
				.requestMatchers(HttpMethod.GET, "/api/v1/auctions/**").permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
				.requestMatchers(HttpMethod.GET, "/sse/**").permitAll()
				.anyRequest().authenticated()
			);

		http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
		http.addFilterBefore(refreshTokenFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
