package org.com.drop.domain.auction.product.qna.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.com.drop.domain.auction.product.controller.ProductController;
import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.auction.product.qna.dto.ProductQnAAnswerRequest;
import org.com.drop.domain.auction.product.qna.dto.ProductQnACreateRequest;
import org.com.drop.domain.auction.product.qna.entity.Answer;
import org.com.drop.domain.auction.product.qna.entity.Question;
import org.com.drop.domain.auction.product.qna.repository.AnswerRepository;
import org.com.drop.domain.auction.product.qna.repository.QuestionRepository;
import org.com.drop.domain.auction.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import jakarta.transaction.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class QnaControllerTest {

	private final Long productId = 1L;
	private final Long questionId = 1L;
	private final Long answerId = 1L;
	private final Long wrongProductId = Long.MAX_VALUE;
	private final Long wrongQuestionId = Long.MAX_VALUE;
	private final Long wrongAnswerId = Long.MAX_VALUE;
	private final String question = "테스트 질의 내용";
	private final String answer = "테스트 답변 내용";
	private String jsonContent;

	@Autowired
	private MockMvc mvc;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private QuestionRepository questionRepository;
	@Autowired
	private AnswerRepository answerRepository;
	@Autowired
	private ProductRepository productRepository;

	void setUp(Object testRequestDto) throws Exception {
		jsonContent = objectMapper.writeValueAsString(testRequestDto);
	}

	@Nested
	class QnATest {
		@Nested
		class QuestionTest {
			@Nested
			class Create {
				@Test
				@DisplayName("질문 등록 - 성공")
				void t1() throws Exception {
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
						.andExpect(jsonPath("$.message").value("요청을 성공적으로 처리했습니다."));

					resultActions
						.andExpect(jsonPath("$.data.qnaId").isNotEmpty())
						.andExpect(jsonPath("$.data.questionerId").isNotEmpty())
						.andExpect(jsonPath("$.data.question").value(question))
						.andExpect(jsonPath("$.data.questionedAt").isNotEmpty());

				}

				@Test
				@DisplayName("질문 등록 - 실패 (내용 없음)")
				void t1_1() throws Exception {
					ProductQnACreateRequest productQnACreateRequest = new ProductQnACreateRequest("");
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
						.andExpect(status().is(400));
				}

				@Test
				@DisplayName("질문 등록 - 실패 (상품 없음)")
				void t1_2() throws Exception {
					ProductQnACreateRequest productQnACreateRequest = new ProductQnACreateRequest(question);
					setUp(productQnACreateRequest);

					//TODO: 로그인 구현 후 인증 확인 수정 필요
					ResultActions resultActions = mvc
						.perform(
							post("/api/v1/products/%d/qna".formatted(wrongProductId))
								.contentType(MediaType.APPLICATION_JSON)
								.content(jsonContent)
						)
						.andDo(print());

					resultActions
						.andExpect(handler().handlerType(QnAController.class))
						.andExpect(handler().methodName("addQna"))
						.andExpect(status().is(404))
						.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
						.andExpect(jsonPath("$.message").value("요청하신 상품 ID를 찾을 수 없습니다."));
				}
			}
		}

		@Nested
		class AnswerTest {
			@Nested
			class Create {
				@Test
				@DisplayName("답변 등록 - 성공")
				void t2() throws Exception {
					ProductQnAAnswerRequest productQnAAnswerRequest = new ProductQnAAnswerRequest(answer);
					setUp(productQnAAnswerRequest);

					//TODO: 로그인 구현 후 인증 확인 수정 필요
					ResultActions resultActions = mvc
						.perform(
							post("/api/v1/products/%d/qna/%d".formatted(productId, questionId))
								.contentType(MediaType.APPLICATION_JSON)
								.content(jsonContent)
						)
						.andDo(print());

					resultActions
						.andExpect(handler().handlerType(QnAController.class))
						.andExpect(handler().methodName("addAnswer"))
						.andExpect(status().isOk())
						.andExpect(jsonPath("$.code").value("SUCCESS"))
						.andExpect(jsonPath("$.message").value("요청을 성공적으로 처리했습니다."));

					resultActions
						.andExpect(jsonPath("$.data.qnaId").isNotEmpty())
						.andExpect(jsonPath("$.data.answererId").isNotEmpty())
						.andExpect(jsonPath("$.data.answer").value(answer))
						.andExpect(jsonPath("$.data.answeredAt").isNotEmpty());

				}

				@Test
				@DisplayName("답변 등록 - 실패 (내용 없음)")
				void t2_1() throws Exception {
					ProductQnAAnswerRequest productQnAAnswerRequest = new ProductQnAAnswerRequest("");
					setUp(productQnAAnswerRequest);

					//TODO: 로그인 구현 후 인증 확인 수정 필요
					ResultActions resultActions = mvc
						.perform(
							post("/api/v1/products/%d/qna/%d".formatted(productId, questionId))
								.contentType(MediaType.APPLICATION_JSON)
								.content(jsonContent)
						)
						.andDo(print());

					resultActions
						.andExpect(handler().handlerType(QnAController.class))
						.andExpect(handler().methodName("addAnswer"))
						.andExpect(status().is(400));
				}

				@Test
				@DisplayName("답변 등록 - 실패 (상품 없음)")
				void t2_2() throws Exception {
					ProductQnAAnswerRequest productQnAAnswerRequest = new ProductQnAAnswerRequest(answer);
					setUp(productQnAAnswerRequest);

					//TODO: 로그인 구현 후 인증 확인 수정 필요
					ResultActions resultActions = mvc
						.perform(
							post("/api/v1/products/%d/qna/%d".formatted(wrongProductId, questionId))
								.contentType(MediaType.APPLICATION_JSON)
								.content(jsonContent)
						)
						.andDo(print());

					resultActions
						.andExpect(handler().handlerType(QnAController.class))
						.andExpect(handler().methodName("addAnswer"))
						.andExpect(status().is(404))
						.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
						.andExpect(jsonPath("$.message").value("요청하신 상품 ID를 찾을 수 없습니다."));
				}

				@Test
				@DisplayName("답변 등록 - 실패 (질문 없음)")
				void t2_3() throws Exception {
					ProductQnAAnswerRequest productQnAAnswerRequest = new ProductQnAAnswerRequest(answer);
					setUp(productQnAAnswerRequest);

					//TODO: 로그인 구현 후 인증 확인 수정 필요
					ResultActions resultActions = mvc
						.perform(
							post("/api/v1/products/%d/qna/%d".formatted(productId, wrongQuestionId))
								.contentType(MediaType.APPLICATION_JSON)
								.content(jsonContent)
						)
						.andDo(print());

					resultActions
						.andExpect(handler().handlerType(QnAController.class))
						.andExpect(handler().methodName("addAnswer"))
						.andExpect(status().is(404))
						.andExpect(jsonPath("$.code").value("PRODUCT_QUESTION_NOT_FOUND"))
						.andExpect(jsonPath("$.message").value("질문을 찾을 수 없습니다."));
				}
			}

			@Nested
			class Delete {
				@Test
				@DisplayName("답변 삭제 - 성공")
				void t3() throws Exception {
					//TODO: 로그인 구현 후 인증 확인 수정 필요
					ResultActions resultActions = mvc
						.perform(
							delete("/api/v1/products/%d/qna/%d".formatted(productId, answerId))
						)
						.andDo(print());

					resultActions
						.andExpect(handler().handlerType(QnAController.class))
						.andExpect(handler().methodName("deleteAnswer"))
						.andExpect(status().isOk())
						.andExpect(jsonPath("$.code").value("SUCCESS"))
						.andExpect(jsonPath("$.message").value("요청을 성공적으로 처리했습니다."));

					Answer answer1 = answerRepository.findById(productId).get();
					assertThat(answer1.getDeletedAt()).isNotNull();
				}

				@Test
				@DisplayName("상품 삭제 - 실패 (상품 없음)")
				void t3_1() throws Exception {
					//TODO: 로그인 구현 후 인증 확인 수정 필요
					ResultActions resultActions = mvc
						.perform(
							delete("/api/v1/products/%d".formatted(wrongProductId))
						)
						.andDo(print());

					resultActions
						.andExpect(handler().handlerType(ProductController.class))
						.andExpect(handler().methodName("deleteProduct"))
						.andExpect(status().isNotFound())
						.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
						.andExpect(jsonPath("$.message").value("요청하신 상품 ID를 찾을 수 없습니다."));
				}
			}

			@Nested
			class Read {
				@Test
				@DisplayName("QnA 조회 - 성공")
				void t4() throws Exception {
					ResultActions resultActions = mvc
						.perform(
							get("/api/v1/products/%d/qna".formatted(productId))
						)
						.andDo(print());

					resultActions
						.andExpect(handler().handlerType(QnAController.class))
						.andExpect(handler().methodName("getQna"))
						.andExpect(status().isOk())
						.andExpect(jsonPath("$.code").value("SUCCESS"))
						.andExpect(jsonPath("$.status").value(200))
						.andExpect(jsonPath("$.message").value("요청을 성공적으로 처리했습니다."));

					Product product = productRepository.findById(productId).get();
					List<Question> questions = questionRepository.findByProductOrderById(product);

					assertThat(jsonPath("$.data.totalCount").value(questions.size()));

					for (int i = 0; i < questions.size(); i++) {
						Question question  = questions.get(i);
						resultActions.andExpect(
							jsonPath("$.data.productQnAResponses[%d].productQnaCreateResponse.qnaId".formatted(i))
								.value(question .getId())
						);
						resultActions.andExpect(
							jsonPath("$.data.productQnAResponses[%d].productQnaCreateResponse.questionerId"
								.formatted(i))
								.value(question .getQuestioner().getId())
						);
						resultActions.andExpect(
							jsonPath("$.data.productQnAResponses[%d].productQnaCreateResponse.question"
								.formatted(i))
								.value(question .getQuestion())
						);
						resultActions.andExpect(
							jsonPath("$.data.productQnAResponses[%d].productQnaCreateResponse.questionedAt"
								.formatted(i))
								.value(question .getCreatedAt().toString())
						);
						List<Answer> answers = answerRepository.findByQuestion(question);

						for (int j = 0; j < answers.size(); j++) {
							Answer answer = answers.get(j);

							resultActions.andExpect(
								jsonPath("$.data.productQnAResponses[%d].answers[%d].qnaId"
									.formatted(i, j))
									.value(answer.getQuestion().getId())
							);

							resultActions.andExpect(
								jsonPath("$.data.productQnAResponses[%d].answers[%d].answerId"
									.formatted(i, j))
									.value(answer.getId())
							);

							resultActions.andExpect(
								jsonPath("$.data.productQnAResponses[%d].answers[%d].answererId"
									.formatted(i, j))
									.value(answer.getAnswerer().getId())
							);

							resultActions.andExpect(
								jsonPath("$.data.productQnAResponses[%d].answers[%d].answer"
									.formatted(i, j))
									.value(answer.getAnswer())
							);

							resultActions.andExpect(
								jsonPath("$.data.productQnAResponses[%d].answers[%d].answeredAt"
									.formatted(i, j))
									.value(answer.getCreatedAt().toString())
							);
						}

					}
				}
			}
		}
	}

}
