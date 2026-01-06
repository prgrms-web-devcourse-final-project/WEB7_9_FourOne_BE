package org.com.drop.domain.payment.method.controller;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.com.drop.domain.payment.method.dto.RegisterCardRequest;
import org.com.drop.domain.payment.method.entity.PaymentMethod;
import org.com.drop.domain.payment.method.repository.PaymentMethodRepository;
import org.com.drop.domain.payment.method.service.PaymentMethodService;
import org.com.drop.domain.payment.payment.domain.CardCompany;
import org.com.drop.domain.user.controller.UserController;
import org.com.drop.domain.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class PaymentMethodControllerTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private PaymentMethodService paymentMethodService;
	@Autowired
	private PaymentMethodRepository paymentMethodRepository;
	@Autowired
	private UserService userService;
	@Autowired
	private ObjectMapper objectMapper;
	private String jsonContent;
	private String billingKey = "bill_12300";
	private CardCompany cardCompany = CardCompany.SAMSUNG;
	private String cardNumberMasked = "0034-****-****-5678";
	private String cardName = "MyCard123";

	@Nested
	class CardRegisterTest {
		@Test
		@DisplayName("카드 등록 - 성공")
		@WithMockUser(username = "user1@example.com", roles = {"USER"})
		void t1() throws Exception {
			RegisterCardRequest request = new RegisterCardRequest(
				billingKey, cardCompany, cardNumberMasked, cardName
			);

			jsonContent = objectMapper.writeValueAsString(request);

			ResultActions resultActions = mockMvc.perform(
					post("/api/v1/user/me/paymentMethods")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonContent)
				)
				.andDo(print());

			resultActions
				.andExpect(handler().handlerType(UserController.class))
				.andExpect(handler().methodName("register"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("SUCCESS"))
				.andExpect(jsonPath("$.message").value("요청을 성공적으로 처리했습니다."));

			PaymentMethod paymentMethod = paymentMethodRepository.findByBillingKey(billingKey).get();

			resultActions
				.andExpect(jsonPath("$.data.id").value(paymentMethod.getId()))
				.andExpect(jsonPath("$.data.billingKey").value(billingKey))
				.andExpect(jsonPath("$.data.cardCompany").value(cardCompany.toString()))
				.andExpect(jsonPath("$.data.cardNumberMasked").value(cardNumberMasked))
				.andExpect(jsonPath("$.data.createdAt").isNotEmpty());

			assertThat(paymentMethod.getBillingKey()).isEqualTo(billingKey);
			assertThat(paymentMethod.getCardCompany()).isEqualTo(cardCompany);
			assertThat(paymentMethod.getCardNumberMasked()).isEqualTo(cardNumberMasked);
		}

		@Test
		@DisplayName("카드 등록 - 실패 - 로그인 없음")
		void t1_1() throws Exception {
			RegisterCardRequest request = new RegisterCardRequest(
				billingKey, cardCompany, cardNumberMasked, cardName
			);

			jsonContent = objectMapper.writeValueAsString(request);

			ResultActions resultActions = mockMvc.perform(
					post("/api/v1/user/me/paymentMethods")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonContent)
				)
				.andDo(print());

			resultActions
				.andExpect(handler().handlerType(UserController.class))
				.andExpect(handler().methodName("register"))
				.andExpect(status().is(401))
				.andExpect(jsonPath("$.code").value("USER_UNAUTHORIZED"))
				.andExpect(jsonPath("$.httpStatus").value("401"))
				.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
		}

		@Test
		@DisplayName("카드 등록 - 실패 - billingKey 없음")
		@WithMockUser(username = "user1@example.com", roles = {"USER"})
		void t1_2() throws Exception {
			RegisterCardRequest request = new RegisterCardRequest(
				"", cardCompany, cardNumberMasked, cardName
			);

			jsonContent = objectMapper.writeValueAsString(request);

			ResultActions resultActions = mockMvc.perform(
					post("/api/v1/user/me/paymentMethods")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonContent)
				)
				.andDo(print());

			resultActions
				.andExpect(handler().handlerType(UserController.class))
				.andExpect(handler().methodName("register"))
				.andExpect(status().is(400))
				.andExpect(jsonPath("$.code").value("USER_PAYMENT_METHOD_INVALID_BILLING_KEY"))
				.andExpect(jsonPath("$.httpStatus").value("400"))
				.andExpect(jsonPath("$.message").value("잘못된 billingKey 입니다."));
		}

		@Test
		@DisplayName("카드 등록 - 실패 - cardCompany 없음")
		@WithMockUser(username = "user1@example.com", roles = {"USER"})
		void t1_3() throws Exception {
			RegisterCardRequest request = new RegisterCardRequest(
				billingKey, null, cardNumberMasked, cardName
			);

			jsonContent = objectMapper.writeValueAsString(request);

			ResultActions resultActions = mockMvc.perform(
					post("/api/v1/user/me/paymentMethods")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonContent)
				)
				.andDo(print());

			resultActions
				.andExpect(handler().handlerType(UserController.class))
				.andExpect(handler().methodName("register"))
				.andExpect(status().is(400))
				.andExpect(jsonPath("$.code").value("USER_PAYMENT_METHOD_INVALID_CARD_COMPANY"))
				.andExpect(jsonPath("$.httpStatus").value("400"))
				.andExpect(jsonPath("$.message").value("잘못된 cardCompany 입니다."));
		}

		@Test
		@DisplayName("카드 등록 - 실패 - cardNumberMasked 없음")
		@WithMockUser(username = "user1@example.com", roles = {"USER"})
		void t1_4() throws Exception {
			RegisterCardRequest request = new RegisterCardRequest(
				billingKey, cardCompany, "", cardName
			);

			jsonContent = objectMapper.writeValueAsString(request);

			ResultActions resultActions = mockMvc.perform(
					post("/api/v1/user/me/paymentMethods")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonContent)
				)
				.andDo(print());

			resultActions
				.andExpect(handler().handlerType(UserController.class))
				.andExpect(handler().methodName("register"))
				.andExpect(status().is(400))
				.andExpect(jsonPath("$.code").value("USER_PAYMENT_METHOD_INVALID_CARD_NUMBER_MASKED"))
				.andExpect(jsonPath("$.httpStatus").value("400"))
				.andExpect(jsonPath("$.message").value("잘못된 cardNumberMasked 입니다."));
		}

		@Test
		@DisplayName("카드 등록 - 실패 - cardName 없음")
		@WithMockUser(username = "user1@example.com", roles = {"USER"})
		void t1_5() throws Exception {
			RegisterCardRequest request = new RegisterCardRequest(
				billingKey, cardCompany, cardNumberMasked, ""
			);

			jsonContent = objectMapper.writeValueAsString(request);

			ResultActions resultActions = mockMvc.perform(
					post("/api/v1/user/me/paymentMethods")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonContent)
				)
				.andDo(print());

			resultActions
				.andExpect(handler().handlerType(UserController.class))
				.andExpect(handler().methodName("register"))
				.andExpect(status().is(400))
				.andExpect(jsonPath("$.code").value("USER_PAYMENT_METHOD_INVALID_CARD_NAME"))
				.andExpect(jsonPath("$.httpStatus").value("400"))
				.andExpect(jsonPath("$.message").value("잘못된 cardName 입니다."));
		}

		@Test
		@DisplayName("카드 등록 - 실패 - 이미 등록됨")
		@WithMockUser(username = "user1@example.com", roles = {"USER"})
		void t1_6() throws Exception {
			RegisterCardRequest request = new RegisterCardRequest(
				billingKey, cardCompany, cardNumberMasked, cardName
			);

			jsonContent = objectMapper.writeValueAsString(request);

			mockMvc.perform(
				post("/api/v1/user/me/paymentMethods")
					.contentType(MediaType.APPLICATION_JSON)
					.content(jsonContent)
			);

			ResultActions resultActions = mockMvc.perform(
					post("/api/v1/user/me/paymentMethods")
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonContent)
				)
				.andDo(print());

			resultActions
				.andExpect(handler().handlerType(UserController.class))
				.andExpect(handler().methodName("register"))
				.andExpect(status().is(409))
				.andExpect(jsonPath("$.code").value("USER_PAYMENT_METHOD_ALREADY_EXISTS"))
				.andExpect(jsonPath("$.httpStatus").value("409"))
				.andExpect(jsonPath("$.message").value("이미 등록된 카드입니다."));
		}
	}

	@Nested
	class CardList {
		@Test
		@DisplayName("카드 목록 조회 - 성공")
		@WithMockUser(username = "user1@example.com", roles = {"USER"})
		void t2() throws Exception {

			ResultActions resultActions = mockMvc
				.perform(
					get("/api/v1/user/me/paymentMethods")
				)
				.andDo(print());

			resultActions
				.andExpect(handler().handlerType(UserController.class))
				.andExpect(handler().methodName("cardList"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("SUCCESS"))
				.andExpect(jsonPath("$.message").value("요청을 성공적으로 처리했습니다."));

			List<PaymentMethod> paymentMethods = paymentMethodRepository.findByUserId(1L);

			for (int i = 0; i < paymentMethods.size(); i++) {
				resultActions
					.andExpect(jsonPath("$.data.registerCardResponses[0].id".formatted(i))
						.value(paymentMethods.get(i).getId()))
					.andExpect(jsonPath("$.data.registerCardResponses[0].cardCompany".formatted(i))
						.value(paymentMethods.get(i).getCardCompany().toString()))
					.andExpect(jsonPath("$.data.registerCardResponses[0].cardNumberMasked".formatted(i))
						.value(paymentMethods.get(i).getCardNumberMasked()))
					.andExpect(jsonPath("$.data.registerCardResponses[0].createdAt".formatted(i))
						.isNotEmpty());
			}
		}

		@Test
		@DisplayName("카드 목록 조회 - 실패 -로그인 없음")
		void t2_1() throws Exception {

			ResultActions resultActions = mockMvc
				.perform(
					get("/api/v1/user/me/paymentMethods")
				)
				.andDo(print());

			resultActions
				.andExpect(handler().handlerType(UserController.class))
				.andExpect(handler().methodName("cardList"))
				.andExpect(status().is(401))
				.andExpect(jsonPath("$.code").value("USER_UNAUTHORIZED"))
				.andExpect(jsonPath("$.httpStatus").value("401"))
				.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
		}
	}

	@Nested
	class CardDelete {
		Long cardId = 1L;
		@Test
		@DisplayName("카드 삭제 - 성공")
		@WithMockUser(username = "user1@example.com", roles = {"USER"})
		void t2() throws Exception {
			PaymentMethod paymentMethod = paymentMethodRepository.findById(cardId).get();

			ResultActions resultActions = mockMvc
				.perform(
					delete("/api/v1/user/me/paymentMethods/%d".formatted(cardId))
				)
				.andDo(print());

			resultActions
				.andExpect(handler().handlerType(UserController.class))
				.andExpect(handler().methodName("delete"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("SUCCESS"))
				.andExpect(jsonPath("$.message").value("요청을 성공적으로 처리했습니다."));

			boolean exist = paymentMethodRepository.findByBillingKey(billingKey).isPresent();

			assertThat(exist).isEqualTo(false);
		}

		@Test
		@DisplayName("카드 삭제 - 실패 -로그인 없음")
		void t2_1() throws Exception {

			ResultActions resultActions = mockMvc
				.perform(
					delete("/api/v1/user/me/paymentMethods/%d".formatted(cardId))
				)
				.andDo(print());

			resultActions
				.andExpect(handler().handlerType(UserController.class))
				.andExpect(handler().methodName("delete"))
				.andExpect(status().is(401))
				.andExpect(jsonPath("$.code").value("USER_UNAUTHORIZED"))
				.andExpect(jsonPath("$.httpStatus").value("401"))
				.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
		}
	}




}
