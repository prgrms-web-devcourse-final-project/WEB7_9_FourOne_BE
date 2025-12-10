package org.com.drop.domain.auction.product.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.auction.product.repository.ProductRepository;
import org.com.drop.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import jakarta.transaction.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class ProductControllerTest {

	private final String name = "상품명";
	private final String description = "상품 설명";
	private final Product.Category category = Product.Category.STARGOODS;
	private final Product.SubCategory subCategory = Product.SubCategory.ACC;
	private final Integer startPrice = 10000;
	private final Integer minBidStep = 100;
	private final LocalDateTime startAt = LocalDateTime.now().plusDays(1);
	private final LocalDateTime endAt = LocalDateTime.now().plusDays(2);
	private final Integer buyNowPrice = 20000;
	private final List<String> images = List.of("img1.png", "img2.png");
	@Autowired
	private MockMvc mvc;
	@Autowired
	private ProductRepository productRepository;
	private User testUser;

	@Test
	@DisplayName("상품 출품 - 성공")
	void t1() throws Exception {
		String jsonContent = String.format(
			"""
					{
						"name": "%s",
						"description" : %s,
						"category" : %s,
						"subcategory" : %s,
						"startPrice" : %s,
						"minBidStep" : %s,
						"startAt" : %s,
						"endAt" : %s,
						"buyNowPrice" : %s,
						"imagesFiles" : %s
					}
					""",
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

		//TODO: 로그인 구현 후 수정 필요

		ResultActions resultActions = mvc
			.perform(
				put("/api/v1/products")
					.contentType(MediaType.APPLICATION_JSON)
					.content(jsonContent)
			)
			.andDo(print());

		resultActions
			.andExpect(handler().handlerType(ProductController.class))
			.andExpect(handler().methodName("addProduct"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.code").value("SUCCESS"))
			.andExpect(jsonPath("$.message").value("요청을 성공적으로 처리했습니다."));

		resultActions
			.andExpect(jsonPath("$.data.name").value(name))
			.andExpect(jsonPath("$.data.description").value(description))
			.andExpect(jsonPath("$.data.category").value(category.name()))
			.andExpect(jsonPath("$.data.subcategory").value(subCategory.name()))
			.andExpect(jsonPath("$.data.startPrice").value(startPrice))
			.andExpect(jsonPath("$.data.minBidStep").value(minBidStep))
			.andExpect(jsonPath("$.data.buyNowPrice").value(buyNowPrice))

			.andExpect(jsonPath("$.data.imagesFiles[0]").value("img1.png"))
			.andExpect(jsonPath("$.data.imagesFiles[1]").value("img2.png"));
	}

	@Test
	@DisplayName("상품 출품 - 실패 - 필수값(이름) 누락")
	void t1_1() throws Exception {
		String jsonContent = String.format(
			"""
					{
						"description" : %s,
						"category" : %s,
						"subcategory" : %s,
						"startPrice" : %s,
						"minBidStep" : %s,
						"startAt" : %s,
						"endAt" : %s,
						"buyNowPrice" : %s,
						"imagesFiles" : %s
					}
					""",
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

		//TODO: 로그인 구현 후 수정 필요

		ResultActions resultActions = mvc
			.perform(
				put("/api/v1/products")
					.contentType(MediaType.APPLICATION_JSON)
					.content(jsonContent)
			)
			.andDo(print());

		resultActions
			.andExpect(handler().handlerType(ProductController.class))
			.andExpect(handler().methodName("addProduct"))
			.andExpect(jsonPath("$.status").value(1205))
			.andExpect(jsonPath("$.code").value("PRODUCT_INVALID_PRODUCT"))
			.andExpect(jsonPath("$.message").value("이름, 설명, 카테고리, 시작가, 최소입찰가, 상품 이미지는 필수 항목 입니다."));
	}
}
