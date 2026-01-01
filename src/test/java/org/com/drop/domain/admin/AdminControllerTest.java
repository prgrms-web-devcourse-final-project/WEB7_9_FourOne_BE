package org.com.drop.domain.admin;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.com.drop.domain.admin.guide.controller.GuideController;
import org.com.drop.domain.admin.guide.dto.GuideCreateRequest;
import org.com.drop.domain.admin.guide.entity.Guide;
import org.com.drop.domain.admin.guide.repository.GuideRepository;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class AdminControllerTest {
	@Autowired
	private MockMvc mvc;
	@Autowired
	private GuideRepository guideRepository;

	@Nested
	class GuideTest {
		private String jsonContent;
		@Autowired
		private ObjectMapper objectMapper;
		@Autowired
		private UserRepository userRepository;
		@Autowired
		private PasswordEncoder passwordEncoder;

		private String guideContent = "TestGuide";
		private String newGuideContent = "NewTestGuide";
		void setUp(String content) throws JsonProcessingException {
			GuideCreateRequest testRequestDto = new GuideCreateRequest(
				content
			);

			jsonContent = objectMapper.writeValueAsString(testRequestDto);
		}

		@BeforeEach
		void setUpUser() {
			User user = User.builder()
				.email("testAdmin@example.com")
				.password(passwordEncoder.encode("Test1234!"))
				.nickname("admin")
				.loginType(User.LoginType.LOCAL)
				.role(User.UserRole.ADMIN)
				.createdAt(LocalDateTime.now())
				.penaltyCount(0)
				.build();

			User saved = userRepository.save(user);
		}

		@Nested
		class Read {
			@Test
			@DisplayName("가이드 조회 - 성공")
			@WithMockUser(username = "adminUser", roles = {"ADMIN"})
			void t1() throws Exception {
				ResultActions resultActions = mvc
					.perform(
						get("/api/v1/admin/help")
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(GuideController.class))
					.andExpect(handler().methodName("getGuide"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.code").value("SUCCESS"))
					.andExpect(jsonPath("$.status").value(200))
					.andExpect(jsonPath("$.message").value("요청을 성공적으로 처리했습니다."));

				List<Guide> guides = guideRepository.findAll();

				for (int i = 0; i < guides.size(); i++) {
					resultActions
						.andExpect(jsonPath("$.data.guides[" + i + "].id").value(guides.get(i).getId()))
						.andExpect(jsonPath("$.data.guides[" + i + "].content").value(guides.get(i).getContent()));
				}
			}
		}

		@Nested
		class Create {
			@Test
			@DisplayName("가이드 생성 - 성공")
			@WithMockUser(username = "testAdmin@example.com", roles = {"ADMIN"})
			void t2() throws Exception {

				setUp(guideContent);

				ResultActions resultActions = mvc
					.perform(
						post("/api/v1/admin/help")
							.contentType(MediaType.APPLICATION_JSON)
							.content(jsonContent)
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(GuideController.class))
					.andExpect(handler().methodName("createGuide"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.code").value("SUCCESS"))
					.andExpect(jsonPath("$.status").value(200))
					.andExpect(jsonPath("$.message").value("요청을 성공적으로 처리했습니다."));

				resultActions
					.andExpect(jsonPath("$.data.id").isNotEmpty())
					.andExpect(jsonPath("$.data.content").value(guideContent));
			}

			@Test
			@DisplayName("가이드 생성 - 실패 (권한 없음)")
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			void t2_1() throws Exception {

				setUp(guideContent);

				ResultActions resultActions = mvc
					.perform(
						post("/api/v1/admin/help")
							.contentType(MediaType.APPLICATION_JSON)
							.content(jsonContent)
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(GuideController.class))
					.andExpect(handler().methodName("createGuide"))
					.andExpect(status().is(401))
					.andExpect(jsonPath("$.code").value("GUIDE_UNAUTHORIZED"))
					.andExpect(jsonPath("$.httpStatus").value(401))
					.andExpect(jsonPath("$.message").value("가이드 수정 권한이 없습니다."));
			}

			@Test
			@DisplayName("가이드 생성 - 실패 (로그인 없음)")
			void t2_2() throws Exception {

				setUp(guideContent);

				ResultActions resultActions = mvc
					.perform(
						post("/api/v1/admin/help")
							.contentType(MediaType.APPLICATION_JSON)
							.content(jsonContent)
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(GuideController.class))
					.andExpect(handler().methodName("createGuide"))
					.andExpect(status().is(401))
					.andExpect(jsonPath("$.code").value("USER_UNAUTHORIZED"))
					.andExpect(jsonPath("$.httpStatus").value(401))
					.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
			}

			@Test
			@DisplayName("가이드 생성 - 실패 (내용 없음)")
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			void t2_3() throws Exception {

				setUp("");

				ResultActions resultActions = mvc
					.perform(
						post("/api/v1/admin/help")
							.contentType(MediaType.APPLICATION_JSON)
							.content(jsonContent)
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(GuideController.class))
					.andExpect(handler().methodName("createGuide"))
					.andExpect(status().is(400))
					.andExpect(jsonPath("$.code").value("GUIDE_INVALID_CONTENT"))
					.andExpect(jsonPath("$.httpStatus").value(400))
					.andExpect(jsonPath("$.message").value("가이드 내용이 입력되지 않았습니다."));
			}
		}

		@Nested
		class Update {
			@Test
			@DisplayName("가이드 수정 - 성공")
			@WithMockUser(username = "testAdmin@example.com", roles = {"ADMIN"})
			void t2() throws Exception {

				setUp(newGuideContent);

				ResultActions resultActions = mvc
					.perform(
						put("/api/v1/admin/help/1")
							.contentType(MediaType.APPLICATION_JSON)
							.content(jsonContent)
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(GuideController.class))
					.andExpect(handler().methodName("updateGuide"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.code").value("SUCCESS"))
					.andExpect(jsonPath("$.status").value(200))
					.andExpect(jsonPath("$.message").value("요청을 성공적으로 처리했습니다."));

				resultActions
					.andExpect(jsonPath("$.data.id").value(1))
					.andExpect(jsonPath("$.data.content").value(newGuideContent));

				Guide guide = guideRepository.findById(1L).get();
				assertThat(guide.getContent()).isEqualTo(newGuideContent);
			}

			@Test
			@DisplayName("가이드 수정 - 실패 (권한 없음)")
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			void t3_1() throws Exception {

				setUp(newGuideContent);

				ResultActions resultActions = mvc
					.perform(
						put("/api/v1/admin/help/1")
							.contentType(MediaType.APPLICATION_JSON)
							.content(jsonContent)
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(GuideController.class))
					.andExpect(handler().methodName("updateGuide"))
					.andExpect(status().is(401))
					.andExpect(jsonPath("$.code").value("GUIDE_UNAUTHORIZED"))
					.andExpect(jsonPath("$.httpStatus").value(401))
					.andExpect(jsonPath("$.message").value("가이드 수정 권한이 없습니다."));
			}

			@Test
			@DisplayName("가이드 수정 - 실패 (로그인 없음)")
			void t3_2() throws Exception {

				setUp(newGuideContent);

				ResultActions resultActions = mvc
					.perform(
						put("/api/v1/admin/help/1")
							.contentType(MediaType.APPLICATION_JSON)
							.content(jsonContent)
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(GuideController.class))
					.andExpect(handler().methodName("updateGuide"))
					.andExpect(status().is(401))
					.andExpect(jsonPath("$.code").value("USER_UNAUTHORIZED"))
					.andExpect(jsonPath("$.httpStatus").value(401))
					.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
			}

			@Test
			@DisplayName("가이드 수정 - 실패 (내용 없음)")
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			void t3_3() throws Exception {

				setUp("");

				ResultActions resultActions = mvc
					.perform(
						put("/api/v1/admin/help/1")
							.contentType(MediaType.APPLICATION_JSON)
							.content(jsonContent)
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(GuideController.class))
					.andExpect(handler().methodName("updateGuide"))
					.andExpect(status().is(400))
					.andExpect(jsonPath("$.code").value("GUIDE_INVALID_CONTENT"))
					.andExpect(jsonPath("$.httpStatus").value(400))
					.andExpect(jsonPath("$.message").value("가이드 내용이 입력되지 않았습니다."));
			}
		}
	}
}
