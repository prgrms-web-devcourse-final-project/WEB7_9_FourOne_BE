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

@SpringBootTest(properties = {
	"spring.cloud.aws.s3.region=ap-northeast-2",
	"spring.cloud.aws.credentials.access-key=dummy",
	"spring.cloud.aws.credentials.secret-key=dummy",
	"spring.cloud.aws.stack.auto=false",
	"spring.cloud.aws.s3.bucket=test-bucket",
	"AWS_S3_BUCKET=test-bucket"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AuthController 슬라이스 테스트")
class AuthControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@MockitoBean
	AuthService authService;

	@Test
	@DisplayName("회원가입 성공 - 201 응답 규격 확인")
	void signup_success() throws Exception {
		LocalSignUpRequest request = new LocalSignUpRequest("test@test.com", "Password123!", "nick");

		given(authService.signup(any())).willReturn(new LocalSignUpResponse(1L, "test@test.com", "nick"));

		mockMvc.perform(post("/api/v1/auth/local/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk()) // RsData 감싸기 때문에 HTTP는 200
			.andExpect(jsonPath("$.status").value(201)) // 내부 데이터는 201
			.andExpect(jsonPath("$.data.email").value("test@test.com"));
	}

	@Test
	@DisplayName("로그인 성공 - 쿠키 및 응답 데이터 확인")
	void login_success() throws Exception {
		LocalLoginRequest request = new LocalLoginRequest("test@test.com", "Password123!");

		given(authService.login(any())).willReturn(new LocalLoginResponse(1L, "test@test.com", "tester", "AT", 3600L));
		given(authService.createRefreshToken(anyString())).willReturn("RT");

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(cookie().exists("refreshToken"))
			.andExpect(jsonPath("$.data.accessToken").value("AT"));
	}

	@Test
	@WithMockUser
	@DisplayName("로그아웃 성공 - 쿠키 만료")
	void logout_success() throws Exception {
		mockMvc.perform(post("/api/v1/auth/logout"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(cookie().maxAge("refreshToken", 0));
	}
}
