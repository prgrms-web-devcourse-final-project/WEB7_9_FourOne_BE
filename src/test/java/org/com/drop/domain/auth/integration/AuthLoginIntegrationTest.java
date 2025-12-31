package org.com.drop.domain.auth.integration;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class AuthLoginIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	StringRedisTemplate stringRedisTemplate;

	@MockitoBean
	ValueOperations<String, String> valueOperations;

	@Autowired
	UserRepository userRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
	}

	@Test
	@DisplayName("AccessToken으로 인증된 요청 성공 (JwtFilter 통과)")
	void accessToken_auth_success() throws Exception {
		String rawPassword = "Password123!";

		User user = User.builder()
			.email("test@test.com")
			.password(passwordEncoder.encode(rawPassword))
			.nickname("tester")
			.loginType(User.LoginType.LOCAL)
			.role(User.UserRole.USER)
			.build();

		userRepository.saveAndFlush(user);

		MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
				{
					"email": "test@test.com",
					"password": "Password123!"
				}
			""")
				.with(csrf()))
			.andExpect(status().isOk())
			.andReturn();

		String accessToken = JsonPath.read(
			loginResult.getResponse().getContentAsString(),
			"$.data.accessToken"
		);

		mockMvc.perform(get("/api/v1/auth/me")
				.header("Authorization", "Bearer " + accessToken))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("로그인 성공")
	void auth_flow_success() throws Exception {

		String rawPassword = "Password123!";

		User user = User.builder()
			.email("test@test.com")
			.password(passwordEncoder.encode(rawPassword))
			.nickname("nick")
			.loginType(User.LoginType.LOCAL)
			.role(User.UserRole.USER)
			.build();

		userRepository.save(user);

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"email": "test@test.com",
						"password": "Password123!"
					}
				""")
				.with(csrf())
			)
			.andExpect(status().isOk())
			.andExpect(cookie().exists("refreshToken"))
			.andExpect(jsonPath("$.data.accessToken").exists());
	}

	@Test
	@DisplayName("로그인 실패 - 비밀번호 불일치")
	void login_fail_invalid_password() throws Exception {
		User user = User.builder()
			.email("fail@test.com")
			.password(passwordEncoder.encode("correct-password"))
			.nickname("failer")
			.loginType(User.LoginType.LOCAL)
			.role(User.UserRole.USER)
			.build();
		userRepository.saveAndFlush(user);

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
						"email": "fail@test.com",
						"password": "wrong-password"
					}
				"""))
			.andExpect(status().isUnauthorized());
	}
}
