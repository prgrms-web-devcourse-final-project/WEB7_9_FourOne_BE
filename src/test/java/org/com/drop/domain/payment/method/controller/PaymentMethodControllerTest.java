package org.com.drop.domain.payment.method.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.com.drop.domain.auth.jwt.JwtProvider;
import org.com.drop.domain.payment.method.dto.CardResponse;
import org.com.drop.domain.payment.method.service.PaymentMethodService;
import org.com.drop.domain.payment.payment.domain.CardCompany;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.service.UserService;
import org.com.drop.global.exception.GlobalExceptionHandler;
import org.com.drop.global.security.auth.LoginUserArgumentResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@WebMvcTest(controllers = PaymentMethodController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
	PaymentMethodControllerTest.MockBeans.class,
	PaymentMethodControllerTest.LoginUserResolverTestConfig.class,
	GlobalExceptionHandler.class
})
class PaymentMethodControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PaymentMethodService paymentMethodService;

	@Autowired
	private UserService userService;

	@Autowired
	private RequestMappingHandlerMapping handlerMapping;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
		Mockito.reset(paymentMethodService, userService);
	}

	@Test
	@DisplayName("카드 등록 - 성공")
	void register_returnsOk_andCallsService() throws Exception {
		// given
		User user = Mockito.mock(User.class);
		when(user.getId()).thenReturn(1L);
		when(userService.findUserByEmail("test@example.com")).thenReturn(user);

		UserDetails principal = org.springframework.security.core.userdetails.User
			.withUsername("test@example.com")
			.password("pw")
			.authorities("ROLE_USER")
			.build();

		Authentication auth = new UsernamePasswordAuthenticationToken(
			principal, null, principal.getAuthorities()
		);
		SecurityContextHolder.getContext().setAuthentication(auth);

		String registerPath = findPath(PaymentMethodController.class, "register");

		// when & then
		mockMvc.perform(
				post(registerPath)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
						{
							"billingKey": "bill_123",
							"cardCompany": "SAMSUNG",
							"cardNumberMasked": "1234-****-****-5678",
							"cardName": "MyCard"
						}
						""")
			)
			.andExpect(status().isOk());

		verify(paymentMethodService, times(1))
			.registerCard(eq(1L), any());
	}

	@Test
	@DisplayName("카드 등록 - Body 없으면 400")
	void register_withoutBody_returnsBadRequest() throws Exception {
		// given (로그인만)
		User user = Mockito.mock(User.class);
		when(user.getId()).thenReturn(1L);
		when(userService.findUserByEmail("test@example.com")).thenReturn(user);

		UserDetails principal = org.springframework.security.core.userdetails.User
			.withUsername("test@example.com")
			.password("pw")
			.authorities("ROLE_USER")
			.build();

		Authentication auth = new UsernamePasswordAuthenticationToken(
			principal, null, principal.getAuthorities()
		);
		SecurityContextHolder.getContext().setAuthentication(auth);

		String registerPath = findPath(PaymentMethodController.class, "register");

		// when & then
		mockMvc.perform(
				post(registerPath)
					.contentType(MediaType.APPLICATION_JSON)
			)
			.andExpect(status().isBadRequest());

		verifyNoInteractions(paymentMethodService);
	}

	@Test
	@DisplayName("카드 목록 조회 - 성공")
	void list_returnsOk_andCallsService() throws Exception {
		// given
		User user = Mockito.mock(User.class);
		when(user.getId()).thenReturn(1L);
		when(userService.findUserByEmail("test@example.com")).thenReturn(user);

		when(paymentMethodService.getCards(1L))
			.thenReturn(List.of(
				new CardResponse(10L, CardCompany.SAMSUNG, "1234-****-****-5678", "MyCard")
			));

		UserDetails principal = org.springframework.security.core.userdetails.User
			.withUsername("test@example.com")
			.password("pw")
			.authorities("ROLE_USER")
			.build();

		Authentication auth = new UsernamePasswordAuthenticationToken(
			principal, null, principal.getAuthorities()
		);
		SecurityContextHolder.getContext().setAuthentication(auth);

		String listPath = findPath(PaymentMethodController.class, "list");

		// when & then
		mockMvc.perform(get(listPath))
			.andExpect(status().isOk());

		verify(paymentMethodService, times(1)).getCards(1L);
	}

	@Test
	@DisplayName("카드 삭제 - 성공")
	void delete_returnsOk_andCallsService() throws Exception {
		// given
		Long cardId = 10L;

		User user = Mockito.mock(User.class);
		when(user.getId()).thenReturn(1L);
		when(userService.findUserByEmail("test@example.com")).thenReturn(user);

		UserDetails principal = org.springframework.security.core.userdetails.User
			.withUsername("test@example.com")
			.password("pw")
			.authorities("ROLE_USER")
			.build();

		Authentication auth = new UsernamePasswordAuthenticationToken(
			principal, null, principal.getAuthorities()
		);
		SecurityContextHolder.getContext().setAuthentication(auth);

		String deletePath = findPath(PaymentMethodController.class, "delete");

		// when & then
		mockMvc.perform(delete(deletePath, cardId))
			.andExpect(status().isOk());

		verify(paymentMethodService, times(1)).deleteCard(1L, cardId);
	}

	private String findPath(Class<?> controllerType, String methodName) {
		for (var entry : handlerMapping.getHandlerMethods().entrySet()) {
			var info = entry.getKey();
			var handlerMethod = entry.getValue();

			if (!controllerType.isAssignableFrom(handlerMethod.getBeanType())) {
				continue;
			}

			if (!handlerMethod.getMethod().getName().equals(methodName)) {
				continue;
			}

			if (info.getPathPatternsCondition() != null
				&& !info.getPathPatternsCondition().getPatterns().isEmpty()) {
				return info.getPathPatternsCondition().getPatterns()
					.iterator().next().getPatternString();
			}

			if (info.getPatternsCondition() != null
				&& !info.getPatternsCondition().getPatterns().isEmpty()) {
				return info.getPatternsCondition().getPatterns()
					.iterator().next();
			}

			throw new IllegalStateException("RequestMappingInfo에 path 패턴이 없습니다.");
		}

		throw new IllegalStateException(
			"매핑을 찾을 수 없습니다: " + controllerType.getSimpleName() + "#" + methodName
		);
	}

	@TestConfiguration
	static class MockBeans {

		@Bean
		PaymentMethodService paymentMethodService() {
			return Mockito.mock(PaymentMethodService.class);
		}

		@Bean
		UserService userService() {
			return Mockito.mock(UserService.class);
		}

		@Bean
		JwtProvider jwtProvider() {
			return Mockito.mock(JwtProvider.class);
		}
	}

	@TestConfiguration
	static class LoginUserResolverTestConfig implements WebMvcConfigurer {

		private final UserService userService;

		LoginUserResolverTestConfig(UserService userService) {
			this.userService = userService;
		}

		@Bean
		LoginUserArgumentResolver loginUserArgumentResolver() {
			return new LoginUserArgumentResolver(userService);
		}

		@Override
		public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
			resolvers.add(loginUserArgumentResolver());
		}
	}
}
