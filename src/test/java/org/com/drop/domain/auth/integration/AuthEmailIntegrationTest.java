package org.com.drop.domain.auth.integration;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.internet.MimeMessage;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@ExtendWith(MockitoExtension.class)
class AuthEmailIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	JavaMailSender javaMailSender;

	@Mock
	ValueOperations<String, String> valueOperations;

	@MockitoBean
	StringRedisTemplate stringRedisTemplate;

	@Mock
	MimeMessage mimeMessage;

	@BeforeEach
	void setUp() {
		given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
		given(javaMailSender.createMimeMessage()).willReturn(mimeMessage);
	}

	@Test
	@DisplayName("이메일 인증 코드 발송 성공 - Redis 저장")
	void email_send_success() throws Exception {

		mockMvc.perform(post("/api/v1/auth/email/send-code")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
							"email": "verify@test.com"
						}
					"""))
			.andExpect(status().isAccepted());

		verify(valueOperations, times(1))
			.set(
				startsWith("email_code:"),
				anyString(),
				anyLong(),
				any()
			);

		verify(javaMailSender, times(1)).send(mimeMessage);
	}

	@Test
	@DisplayName("이메일 인증 코드 검증 성공")
	void email_verify_success() throws Exception {

		String email = "verify@test.com";
		String code = "123456";

		given(valueOperations.get("email_code:" + email))
			.willReturn(code);

		mockMvc.perform(post("/api/v1/auth/email/verify-code")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
							"email": "verify@test.com",
							"code": "123456"
						}
					"""))
			.andExpect(status().isOk());

		verify(stringRedisTemplate, times(1))
			.delete("email_code:" + email);
	}

	@Test
	@DisplayName("이메일 인증 실패 - 코드 불일치")
	void email_verify_fail_invalid_code() throws Exception {

		given(valueOperations.get("email_code:verify@test.com"))
			.willReturn("999999");

		mockMvc.perform(post("/api/v1/auth/email/verify-code")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
							"email": "verify@test.com",
							"code": "123456"
						}
					"""))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("이메일 인증 실패 - 인증 코드 만료")
	void email_verify_fail_expired() throws Exception {

		given(valueOperations.get(anyString()))
			.willReturn(null);

		mockMvc.perform(post("/api/v1/auth/email/verify-code")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
					    	"email": "verify@test.com",
					    	"code": "123456"
						}
					"""))
			.andExpect(status().isGone());
	}
}
