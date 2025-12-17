package org.com.drop.domain.auth.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthLoginIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	UserRepository userRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

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
				"""))
			.andExpect(status().isOk())
			.andExpect(cookie().exists("refreshToken"))
			.andExpect(jsonPath("$.data.accessToken").exists());
	}
}
