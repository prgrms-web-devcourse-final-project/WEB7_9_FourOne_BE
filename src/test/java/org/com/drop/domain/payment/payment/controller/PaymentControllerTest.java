package org.com.drop.domain.payment.payment.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.com.drop.domain.auth.jwt.JwtProvider;
import org.com.drop.domain.payment.payment.domain.Payment;
import org.com.drop.domain.payment.payment.service.PaymentService;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.service.UserService;
import org.com.drop.global.exception.ErrorCode;
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

@WebMvcTest(controllers = PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
	PaymentControllerTest.MockBeans.class,
	PaymentControllerTest.LoginUserResolverTestConfig.class,
	GlobalExceptionHandler.class
})
class PaymentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private UserService userService;

	@Autowired
	private JwtProvider jwtProvider;

	@Autowired
	private RequestMappingHandlerMapping handlerMapping;

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
		Mockito.reset(paymentService, userService, jwtProvider);
	}

	@Test
	@DisplayName("결제 생성 - 성공")
	void create_returnsOk() throws Exception {
		// given
		Long winnerId = 55L;
		Long amount = 1000L;

		User user = Mockito.mock(User.class);
		when(user.getId()).thenReturn(1L);

		Payment payment = Mockito.mock(Payment.class);

		when(userService.findUserByEmail("test@example.com")).thenReturn(user);
		when(paymentService.createPayment(winnerId, amount)).thenReturn(payment);

		setAuthentication("test@example.com");

		String createPath = findPath(PaymentController.class, "create");

		// when & then
		mockMvc.perform(
				post(createPath)
					.param("winnerId", String.valueOf(winnerId))
					.param("amount", String.valueOf(amount))
			)
			.andExpect(status().isOk());

		verify(paymentService, times(1)).createPayment(winnerId, amount);
	}

	@Test
	@DisplayName("결제 생성 - amount 누락이면 400")
	void create_withoutAmount_returnsBadRequest() throws Exception {
		// given
		Long winnerId = 55L;

		User user = Mockito.mock(User.class);
		when(user.getId()).thenReturn(1L);
		when(userService.findUserByEmail("test@example.com")).thenReturn(user);

		setAuthentication("test@example.com");

		String createPath = findPath(PaymentController.class, "create");

		// when & then
		mockMvc.perform(
				post(createPath)
					.param("winnerId", String.valueOf(winnerId))
			)
			.andExpect(status().isBadRequest());

		verifyNoInteractions(paymentService);
	}

	@Test
	@DisplayName("결제 생성 - ServiceException 발생 시 GlobalExceptionHandler가 상태코드를 내려준다")
	void create_whenServiceThrowsServiceException_returnsErrorStatus() throws Exception {
		// given
		Long winnerId = 55L;
		Long amount = 1000L;

		User user = Mockito.mock(User.class);
		when(user.getId()).thenReturn(1L);
		when(userService.findUserByEmail("test@example.com")).thenReturn(user);

		doThrow(ErrorCode.PAY_NOT_FOUND_PAYMENT.serviceException("not found"))
			.when(paymentService).createPayment(winnerId, amount);

		setAuthentication("test@example.com");

		String createPath = findPath(PaymentController.class, "create");

		// when & then
		mockMvc.perform(
				post(createPath)
					.param("winnerId", String.valueOf(winnerId))
					.param("amount", String.valueOf(amount))
			)
			.andExpect(status().isNotFound());

		verify(paymentService, times(1)).createPayment(winnerId, amount);
	}

	@Test
	@DisplayName("결제 실패 처리 - 성공")
	void fail_returnsOk_andCallsServiceWithReason() throws Exception {
		// given
		Long paymentId = 55L;

		User user = Mockito.mock(User.class);
		when(user.getId()).thenReturn(1L);
		when(userService.findUserByEmail("test@example.com")).thenReturn(user);

		Payment payment = Mockito.mock(Payment.class);
		when(paymentService.failPayment(paymentId, "buyer_cancel"))
			.thenReturn(payment);

		setAuthentication("test@example.com");

		String failPath = findPath(PaymentController.class, "fail");

		// when & then
		mockMvc.perform(
				post(failPath, paymentId)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
					{"reason":"buyer_cancel"}
					""")
			)
			.andExpect(status().isOk());

		verify(paymentService, times(1)).failPayment(paymentId, "buyer_cancel");
	}

	private void setAuthentication(String email) {
		UserDetails principal = org.springframework.security.core.userdetails.User
			.withUsername(email)
			.password("pw")
			.authorities("ROLE_USER")
			.build();

		Authentication auth = new UsernamePasswordAuthenticationToken(
			principal, null, principal.getAuthorities()
		);
		SecurityContextHolder.getContext().setAuthentication(auth);
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
		PaymentService paymentService() {
			return Mockito.mock(PaymentService.class);
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

