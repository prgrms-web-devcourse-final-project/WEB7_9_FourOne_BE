package org.com.drop.domain.notification.controller;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.com.drop.domain.notification.entity.Notification;
import org.com.drop.domain.notification.repository.NotificationEmitterRepository;
import org.com.drop.domain.notification.repository.NotificationRepository;
import org.com.drop.domain.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.transaction.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@EnableScheduling
public class NotificationControllerTest {
	Long userId = 1L;
	Long notificationId = 1L;
	Long wrongUserId = 2L;
	Long wrongNotificationId = Long.MAX_VALUE;
	@Autowired
	private MockMvc mvc;
	@Autowired
	private NotificationService notificationService;
	@Autowired
	private NotificationEmitterRepository notificationEmitterRepository;
	@Autowired
	private NotificationRepository notificationRepository;

	@Nested
	class Sse {
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

	@Nested
	class NotificationTest {
		@Nested
		class Read {
			@Test
			@DisplayName("알림 조회 단건 - 성공")
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			void t4() throws Exception {
				ResultActions resultActions = mvc
					.perform(
						get("/api/v1/notifications/%d".formatted(notificationId))
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(NotificationController.class))
					.andExpect(handler().methodName("getNotificationById"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.code").value("SUCCESS"))
					.andExpect(jsonPath("$.message").value("요청을 성공적으로 처리했습니다."));

				Notification notification = notificationRepository.findById(notificationId).get();

				resultActions
					.andExpect(jsonPath("$.data.id").value(notification.getId()))
					.andExpect(jsonPath("$.data.userId").value(notification.getUser().getId()))
					.andExpect(jsonPath("$.data.message").value(notification.getMessage()));
			}

			@Test
			@DisplayName("알림 조회 단건 - 실패 (로그인 없음)")
			void t4_1() throws Exception {
				ResultActions resultActions = mvc
					.perform(
						get("/api/v1/notifications/%d".formatted(notificationId))
					)
					.andDo(print());
				resultActions.andExpect(status().isForbidden());
			}

			@Test
			@DisplayName("알림 조회 단건 - 실패 (알림 없음)")
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			void t4_2() throws Exception {
				ResultActions resultActions = mvc
					.perform(
						get("/api/v1/notifications/%d".formatted(wrongNotificationId))
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(NotificationController.class))
					.andExpect(handler().methodName("getNotificationById"))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"))
					.andExpect(jsonPath("$.message").value("알림을 찾을 수 없습니다."));
			}

			@Test
			@DisplayName("알림 조회 단건 - 실패 (권한 없음)")
			@WithMockUser(username = "user2@example.com", roles = {"USER"})
			void t4_3() throws Exception {
				ResultActions resultActions = mvc
					.perform(
						get("/api/v1/notifications/%d".formatted(notificationId))
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(NotificationController.class))
					.andExpect(handler().methodName("getNotificationById"))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"))
					.andExpect(jsonPath("$.message").value("접근 권한이 없습니다."));
			}

			@Test
			@DisplayName("알림 조회 다건 - 성공")
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			void t5() throws Exception {
				ResultActions resultActions = mvc
					.perform(
						get("/api/v1/notifications")
					)
					.andDo(print());

				resultActions.andExpect(status().isOk())
					.andExpect(jsonPath("$.code").value("SUCCESS"))
					.andExpect(jsonPath("$.message").value("요청을 성공적으로 처리했습니다."))
					.andExpect(jsonPath("$.data").isArray())
					.andExpect(jsonPath("$.data.length()").value(2));

				Page<Notification> page =
					notificationRepository.findAllByUserId(
						userId,
						PageRequest.of(0, 20, Sort.by("sendAt").descending())
					);

				List<Notification> notifications = page.getContent();

				for (int i = 0; i < notifications.size(); i++) {
					Notification notif = notifications.get(i);

					resultActions
						.andExpect(jsonPath("$.data[" + i + "].id").value(notif.getId()))
						.andExpect(jsonPath("$.data[" + i + "].userId").value(notif.getUser().getId()))
						.andExpect(jsonPath("$.data[" + i + "].message").value(notif.getMessage()));
				}
			}

			@Test
			@DisplayName("알림 조회 다건 - 실패 (로그인 없음)")
			void t5_1() throws Exception {
				ResultActions resultActions = mvc
					.perform(
						get("/api/v1/notifications/")
					)
					.andDo(print());
				resultActions.andExpect(status().isForbidden());
			}
		}

		@Nested
		class Update {
			@Test
			@DisplayName("알림 읽기 - 성공")
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			void t6() throws Exception {
				ResultActions resultActions = mvc
					.perform(
						put("/api/v1/notifications/%d".formatted(notificationId))
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(NotificationController.class))
					.andExpect(handler().methodName("readNotification"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.code").value("SUCCESS"))
					.andExpect(jsonPath("$.message").value("요청을 성공적으로 처리했습니다."))
					.andExpect(jsonPath("$.data.sendAt").isNotEmpty());

				Notification after = notificationRepository.findById(notificationId).get();
				assertThat(after.getReadAt()).isNotNull();
			}

			@Test
			@DisplayName("알림 읽기 - 실패 (로그인 없음)")
			void t6_1() throws Exception {
				ResultActions resultActions = mvc
					.perform(
						put("/api/v1/notifications/%d".formatted(notificationId))
					)
					.andDo(print());

				resultActions.andExpect(status().isForbidden());
			}

			@Test
			@DisplayName("알림 읽기 - 실패 (알림 없음)")
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			void t6_2() throws Exception {
				ResultActions resultActions = mvc
					.perform(
						put("/api/v1/notifications/%d".formatted(wrongNotificationId))
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(NotificationController.class))
					.andExpect(handler().methodName("readNotification"))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"))
					.andExpect(jsonPath("$.message").value("알림을 찾을 수 없습니다."));
			}

			@Test
			@DisplayName("알림 읽기 - 실패 (권한 없음)")
			@WithMockUser(username = "user2@example.com", roles = {"USER"})
			void t4_3() throws Exception {
				ResultActions resultActions = mvc
					.perform(
						put("/api/v1/notifications/%d".formatted(notificationId))
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(NotificationController.class))
					.andExpect(handler().methodName("readNotification"))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"))
					.andExpect(jsonPath("$.message").value("접근 권한이 없습니다."));
			}
		}

		@Nested
		class Delete {
			@Test
			@DisplayName("알림 삭제 - 성공")
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			void t6() throws Exception {
				ResultActions resultActions = mvc
					.perform(
						delete("/api/v1/notifications/%d".formatted(notificationId))
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(NotificationController.class))
					.andExpect(handler().methodName("deleteNotification"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.code").value("SUCCESS"))
					.andExpect(jsonPath("$.message").value("요청을 성공적으로 처리했습니다."));

				Optional<Notification> after = notificationRepository.findById(notificationId);
				assertThat(after.isPresent()).isFalse();
			}

			@Test
			@DisplayName("알림 삭제 - 실패 (로그인 없음)")
			void t6_1() throws Exception {
				ResultActions resultActions = mvc
					.perform(
						delete("/api/v1/notifications/%d".formatted(notificationId))
					)
					.andDo(print());

				resultActions.andExpect(status().isForbidden());
			}

			@Test
			@DisplayName("알림 삭제 - 실패 (알림 없음)")
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			void t6_2() throws Exception {
				ResultActions resultActions = mvc
					.perform(
						delete("/api/v1/notifications/%d".formatted(wrongNotificationId))
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(NotificationController.class))
					.andExpect(handler().methodName("deleteNotification"))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"))
					.andExpect(jsonPath("$.message").value("알림을 찾을 수 없습니다."));
			}

			@Test
			@DisplayName("알림 삭제 - 실패 (권한 없음)")
			@WithMockUser(username = "user2@example.com", roles = {"USER"})
			void t4_3() throws Exception {
				ResultActions resultActions = mvc
					.perform(
						delete("/api/v1/notifications/%d".formatted(notificationId))
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(NotificationController.class))
					.andExpect(handler().methodName("deleteNotification"))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.code").value("AUTH_ACCESS_DENIED"))
					.andExpect(jsonPath("$.message").value("접근 권한이 없습니다."));
			}
		}

	}

}
