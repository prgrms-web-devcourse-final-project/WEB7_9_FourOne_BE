package org.com.drop.domain.auth.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.com.drop.domain.auth.dto.LocalLoginRequest;
import org.com.drop.domain.auth.dto.LocalLoginResponse;
import org.com.drop.domain.auth.dto.LocalSignUpRequest;
import org.com.drop.domain.auth.dto.LocalSignUpResponse;
import org.com.drop.domain.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AuthController 컨트롤러 통합 테스트")
class AuthControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@MockitoBean
	AuthService authService;

	@Test
	@DisplayName("회원가입 성공")
	void signup_success() throws Exception {
		LocalSignUpRequest request =
			new LocalSignUpRequest("test@test.com", "Password123!", "nick");

		given(authService.signup(any()))
			.willReturn(new LocalSignUpResponse(
				1L,
				"test@test.com",
				"nick"
			));

		mockMvc.perform(post("/api/v1/auth/local/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.email").value("test@test.com"))
			.andExpect(jsonPath("$.data.nickname").value("nick"));
	}


	@Test
	@DisplayName("로그인 성공 - RefreshToken 쿠키 설정")
	void login_success() throws Exception {
		LocalLoginRequest request =
			new LocalLoginRequest("test@test.com", "Password123!");

		given(authService.login(any()))
			.willReturn(new LocalLoginResponse(
				1L,
				"test@test.com",
				"tester",
				"ACCESS_TOKEN",
				3600L
			));

		given(authService.createRefreshToken(anyString()))
			.willReturn("REFRESH_TOKEN");

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(cookie().exists("refreshToken"))
			.andExpect(jsonPath("$.data.accessToken").value("ACCESS_TOKEN"));
	}

	@Test
	@WithMockUser
	@DisplayName("로그아웃 성공 - RefreshToken 쿠키 만료")
	void logout_success() throws Exception {
		mockMvc.perform(post("/api/v1/auth/logout"))
			.andExpect(status().isOk())
			.andExpect(cookie().maxAge("refreshToken", 0));
	}
}
