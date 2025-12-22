package org.com.drop.domain.notification.controller;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

import java.util.concurrent.TimeUnit;

import org.com.drop.domain.notification.repository.NotificationEmitterRepository;
import org.com.drop.domain.notification.service.NotificationService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.transaction.Transactional;

@Disabled
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@EnableScheduling
public class NotificationControllerTest {
	Long userId = 1L;
	@Autowired
	private MockMvc mvc;
	@Autowired
	private NotificationService notificationService;
	@Autowired
	private NotificationEmitterRepository notificationEmitterRepository;

	@Test
	@WithMockUser(username = "user1@example.com", roles = {"USER"})
	@DisplayName("SSE 연결 후 알림 발송 - 성공")
	void t1() throws Exception {
		MvcResult mvcResult = mvc.perform(
			get("/api/v1/notifications/subscribe")
				.param("userId", userId.toString())
				.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
			.andExpect(request().asyncStarted())
			.andReturn();

		notificationService.sendTo(userId, "Hello SSE!");

		String content = mvcResult.getResponse().getContentAsString();
		assertThat(content).contains("Hello SSE!");

		System.out.println("최종 응답 결과: " + mvcResult.getResponse().getContentAsString());
	}

	@Test
	@DisplayName("SSE 연결 종료 후 메모리 검증 - 성공")
	@WithMockUser(username = "user1@example.com", roles = {"USER"})
	void t2() throws Exception {
		MvcResult mvcResult = mvc.perform(get("/api/v1/notifications/subscribe")
				.param("userId", userId.toString()))
			.andExpect(request().asyncStarted())
			.andReturn();

		SseEmitter emitter = notificationEmitterRepository.get(userId);
		emitter.complete();

		mvc.perform(asyncDispatch(mvcResult));

		assertNull(notificationEmitterRepository.get(userId));
	}

	@Test
	@DisplayName("하트비트 전송 - 성공")
	@WithMockUser(username = "user1@example.com", roles = {"USER"})
	void t3() throws Exception {
		MvcResult mvcResult = mvc.perform(
			get("/api/v1/notifications/subscribe")
				.param("userId", userId.toString()))
			.andExpect(request().asyncStarted())
			.andReturn();

		await()
			.atMost(17, TimeUnit.SECONDS)
			.pollInterval(1, TimeUnit.SECONDS)
			.untilAsserted(() -> {
				String content = mvcResult.getResponse().getContentAsString();
				assertThat(content)
					.as("하트비트 메시지가 아직 수신되지 않았습니다. 현재 응답: %s", content)
					.contains(":heartbeat");
			});
		System.out.println("최종 응답 결과: " + mvcResult.getResponse().getContentAsString());
	}
}
