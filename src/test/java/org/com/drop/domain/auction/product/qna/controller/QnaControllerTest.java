package org.com.drop.domain.auction.product.qna.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.com.drop.config.TestSecurityConfig;
import org.com.drop.domain.auction.product.qna.dto.ProductQnACreateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import jakarta.transaction.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class QnaControllerTest {

	private final Long productId = 1L;
	private final Long wrongProductId = Long.MAX_VALUE;
	private final String question = "테스트 질의 내용";
	private final String answer = "테스트 답변 내용";
	private String jsonContent;

	@Autowired
	private MockMvc mvc;
	@Autowired
	private ObjectMapper objectMapper;

	void setUp(Object testRequestDto) throws Exception {
		jsonContent = objectMapper.writeValueAsString(testRequestDto);
	}

	@Nested
	class QnATest {
		@Nested
		class Question {
			@Test
			@DisplayName("질문 등록 - 성공")
			void t4() throws Exception {
				ProductQnACreateRequest productQnACreateRequest = new ProductQnACreateRequest(question);
				setUp(productQnACreateRequest);

				//TODO: 로그인 구현 후 인증 확인 수정 필요
				ResultActions resultActions = mvc
					.perform(
						post("/api/v1/products/%d/qna".formatted(productId))
							.contentType(MediaType.APPLICATION_JSON)
							.content(jsonContent)
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(QnAController.class))
					.andExpect(handler().methodName("addQna"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.code").value("SUCCESS"))
					.andExpect(jsonPath("$.status").value(200))
					.andExpect(jsonPath("$.message").value("요청을 성공적으로 처리했습니다."));

				resultActions
					.andExpect(jsonPath("$.data.qnaId").isNotEmpty())
					.andExpect(jsonPath("$.data.questionerId").isNotEmpty())
					.andExpect(jsonPath("$.data.question").value(question))
					.andExpect(jsonPath("$.data.questionedAt").isNotEmpty());

			}
		}
	}

}
