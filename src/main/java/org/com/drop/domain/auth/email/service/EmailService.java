package org.com.drop.domain.auth.email.service;

import org.com.drop.global.exception.ErrorCode;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

	private final JavaMailSender javaMailSender;

	public void sendVerificationEmail(String toEmail, String code) {

		String subject = "[Drop] 회원가입 인증 코드입니다.";
		String text = buildEmailText(code);

		MimeMessage mimeMessage = javaMailSender.createMimeMessage();

		try {
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

			helper.setTo(toEmail);
			helper.setSubject(subject);
			helper.setText(text, true);

			javaMailSender.send(mimeMessage);

			log.info("인증 코드 이메일 발송 성공: To={}, Code={}", toEmail, code);

		} catch (MessagingException e) {
			log.error("인증 코드 이메일 발송 실패: To={}, Error={}", toEmail, e.getMessage(), e);

			throw ErrorCode.AUTH_EMAIL_SEND_FAILED
				.serviceException("이메일 발송 시스템 오류: To=%s", toEmail);
		}
	}


	private String buildEmailText(String code) {
		return "<html><body>"
			+ "<h2>Drop 회원가입 인증</h2>"
			+ "<p>아래 <b>6자리 인증 코드</b>를 입력창에 넣어 이메일 인증을 완료해 주세요.</p>"
			+ "<div style='background-color:#f0f0f0; padding:10px; border-radius:5px; text-align:center;'>"
			+ "<h3>인증 코드: <span style='color: #4CAF50; font-weight: bold;'>" + code + "</span></h3>"
			+ "</div>"
			+ "<p>본 코드는 5분간 유효합니다.</p>"
			+ "</body></html>";
	}
}
