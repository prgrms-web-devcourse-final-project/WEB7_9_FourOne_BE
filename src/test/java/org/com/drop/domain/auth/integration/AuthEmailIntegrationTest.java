package org.com.drop.domain.auth.integration;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.internet.MimeMessage;

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
@Transactional
class AuthEmailIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	JavaMailSender javaMailSender;

	@MockitoBean
	StringRedisTemplate stringRedisTemplate;

	ValueOperations<String, String> valueOperations;

	@BeforeEach
	void setUp() {
		valueOperations = mock(ValueOperations.class);
		given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);

		MimeMessage mimeMessage = mock(MimeMessage.class);
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
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(202));

		verify(valueOperations, times(1)).set(
			startsWith("email_code:"),
			anyString(),
			anyLong(),
			any()
		);

		verify(javaMailSender, times(1)).send(any(MimeMessage.class));
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
				.content(String.format("""
                {
                   "email": "%s",
                   "code": "%s"
                }
             """, email, code)))
			.andExpect(status().isOk());

		verify(stringRedisTemplate, times(1)).delete("email_code:" + email);
	}

	@Test
	@DisplayName("이메일 인증 실패 - 코드 불일치 (400 예상)")
	void email_verify_fail_invalid_code() throws Exception {
		String email = "verify@test.com";
		given(valueOperations.get("email_code:" + email)).willReturn("999999");

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
	@DisplayName("이메일 인증 실패 - 인증 코드 만료 (410 예상)")
	void email_verify_fail_expired() throws Exception {
		given(valueOperations.get(anyString())).willReturn(null);

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
