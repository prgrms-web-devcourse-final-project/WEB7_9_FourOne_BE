package org.com.drop.domain.auction.product.service;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import java.util.List;

import org.com.drop.domain.auction.product.dto.ProductCreateRequest;
import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.auction.product.repository.ProductImageRepository;
import org.com.drop.domain.auction.product.repository.ProductRepository;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.transaction.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Disabled
public class ProductServiceTest {
	@Autowired
	ProductService productService;
	@Autowired
	ProductRepository productRepository;
	@Autowired
	ProductImageRepository productImageRepository;
	@Autowired
	UserRepository userRepository;

	private User testUser;
	private String testProductName = "new 테스트 상품명";
	private String wrongImageUrl = "잘못된 이미지 url";
	@BeforeEach
	void setUp(){
		testUser = userRepository.findById(1L).get();
	}
	@Nested
	class addProduct
	{
		@Test
		@DisplayName("상품 생성 실패 - 상품 저장 성공, 이미지 저장 실패")
		void t1() {
			List<String> invalidUrls = List.of("invalid-url");
			ProductCreateRequest request = new ProductCreateRequest(
				testProductName, "설명", Product.Category.STARGOODS, Product.SubCategory.ACC, invalidUrls);

			assertThatThrownBy(() -> productService.addProduct(request, testUser))
				.isInstanceOf(RuntimeException.class);

			assertThat(productRepository.findByName(testProductName).size()).isEqualTo(0);
			assertThat(productImageRepository.findByImageUrl(wrongImageUrl).size()).isEqualTo(0);
		}
	}

	@Nested
	class updateProduct
	{
		@Test
		@DisplayName("상품 수정 실패 - 상품 저장 성공, 이미지 저장 실패")
		@Transactional
		void t2() {
			Product product = productService.findProductById(1L);
			List<String> invalidUrls = List.of("invalid-url");
			ProductCreateRequest request = new ProductCreateRequest(
				testProductName, "설명", Product.Category.STARGOODS, Product.SubCategory.ACC, invalidUrls);

			assertThatThrownBy(() -> productService.updateProduct(1L, request, testUser))
				.isInstanceOf(RuntimeException.class);

			assertThat(product.getName()).isNotEqualTo(testProductName);
			assertThat(productImageRepository.findByImageUrl(wrongImageUrl).size()).isEqualTo(0);
		}
	}

	@Nested
	class deleteProduct
	{
		@Test
		@DisplayName("상품 삭제 성공")
		@Transactional
		void t3() {
			productService.deleteProduct(1L, testUser);
			assertThat(productRepository.findById(1L).get().getDeletedAt()).isNotNull();
			assertThat(productImageRepository.findAllByProductId(1L).size()).isEqualTo(0);
		}
	}
}
