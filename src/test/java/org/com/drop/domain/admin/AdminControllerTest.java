package org.com.drop.domain.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.com.drop.domain.admin.guide.controller.GuideController;
import org.com.drop.domain.admin.guide.entity.Guide;
import org.com.drop.domain.admin.guide.repository.GuideRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

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

		@Nested
		class Read {
			@Test
			@DisplayName("가이드 조회 - 성공")
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
	}
}
