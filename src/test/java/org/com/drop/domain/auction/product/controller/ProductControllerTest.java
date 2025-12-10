package org.com.drop.domain.auction.product.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.com.drop.config.TestSecurityConfig;
import org.com.drop.domain.auction.product.dto.AuctionCreateRequest;
import org.com.drop.domain.auction.product.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
public class ProductControllerTest {
	private final String name = "테스트 상품명";
	private final String description = "테스트 상품 상세 설명";
	private final Product.Category category = Product.Category.STARGOODS;
	private final Product.SubCategory subCategory = Product.SubCategory.ACC;
	private final Integer startPrice = 10000;
	private final Integer minBidStep = 100;
	private final LocalDateTime startAt = LocalDateTime.now().plusMinutes(10).truncatedTo(ChronoUnit.MILLIS);
	private final LocalDateTime endAt = LocalDateTime.now().plusMinutes(20).truncatedTo(ChronoUnit.MILLIS);
	private final Integer buyNowPrice = 20000;
	private final List<String> images = List.of("img1.png", "img2.png");

	@Autowired
	private MockMvc mvc;
	@Autowired
	private ObjectMapper objectMapper;


	private AuctionCreateRequest testRequestDto;
	private AuctionCreateRequest testFailedRequestDto;
	private String jsonContent;
	private String jsonFailedContent;

	@BeforeEach
	void setUp() throws Exception {
		testRequestDto = new AuctionCreateRequest(
			name,
			description,
			category,
			subCategory,
			startPrice,
			minBidStep,
			startAt,
			endAt,
			buyNowPrice,
			images
		);

		testFailedRequestDto= new AuctionCreateRequest(
			"",
			description,
			category,
			subCategory,
			startPrice,
			minBidStep,
			startAt,
			endAt,
			buyNowPrice,
			images
		);

		jsonContent = objectMapper.writeValueAsString(testRequestDto);
		jsonFailedContent = objectMapper.writeValueAsString(testFailedRequestDto);
	}

	@Test
	@DisplayName("상품 출품 - 성공")
	void t1() throws Exception {
		//TODO: 로그인 구현 후 인증 확인 수정 필요
		ResultActions resultActions = mvc
			.perform(
				post("/api/v1/products")
					.contentType(MediaType.APPLICATION_JSON)
					.content(jsonContent)
			)
			.andDo(print());

		resultActions
			.andExpect(handler().handlerType(ProductController.class))
			.andExpect(handler().methodName("addProduct"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("SUCCESS"))
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.message").value("요청을 성공적으로 처리했습니다."));

		resultActions
			.andExpect(jsonPath("$.data.status").value("SCHEDULED"))
			.andExpect(jsonPath("$.data.startAt").value(startAt.toString()))
			.andExpect(jsonPath("$.data.endAt").value(endAt.toString()))
			.andExpect(jsonPath("$.data.createdAt").isNotEmpty())
			.andExpect(jsonPath("$.data.updatedAt").isEmpty());
	}

	@Test
	@DisplayName("상품 출품 - 실패 - 필수값(이름) 누락")
	void t1_1() throws Exception {
		//TODO: 로그인 구현 후 인증 확인 수정 필요
		ResultActions resultActions = mvc
			.perform(
				post("/api/v1/products")
					.contentType(MediaType.APPLICATION_JSON)
					.content(jsonFailedContent)
			)
			.andDo(print());

		resultActions
			.andExpect(handler().handlerType(ProductController.class))
			.andExpect(handler().methodName("addProduct"))
			.andExpect(status().is(400))
			.andExpect(jsonPath("$.status").value("PRODUCT_INVALID_PRODUCT"))
			.andExpect(jsonPath("$.code").value(1205))
			.andExpect(jsonPath("$.message").value("이름, 설명, 카테고리, 시작가, 최소입찰가, 상품 이미지는 필수 항목 입니다."));

	}
}
