package org.com.drop.domain.auction.product.service;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import java.util.List;

import org.com.drop.BaseIntegrationTest;
import org.com.drop.domain.auction.product.dto.ProductCreateRequest;
import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.auction.product.repository.ProductImageRepository;
import org.com.drop.domain.auction.product.repository.ProductRepository;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import jakarta.transaction.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class ProductServiceTest extends BaseIntegrationTest {

	@Nested
	class TransactionalTests {
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
		void setUp() {
			testUser = userRepository.findById(1L).get();
		}

		@Nested
		class AddProduct {
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
		class UpdateProduct {
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
		class DeleteProduct {
			@Test
			@DisplayName("상품 삭제 성공")
			@Transactional
			void t3() {
				productService.deleteProduct(2L, testUser);
				assertThat(productRepository.findById(2L).get().getDeletedAt()).isNotNull();
				assertThat(productImageRepository.findAllByProductId(2L).size()).isEqualTo(0);
			}
		}
	}

	@Nested
	@Transactional
	class CacheTests {
		private final String name = "테스트 상품명";
		private final String updatedName = "수정된 테스트 상품명";
		private final String description = "테스트 상품 상세 설명";
		private final Product.Category category = Product.Category.STARGOODS;
		private final Product.SubCategory subCategory = Product.SubCategory.ACC;
		private final List<String> images = List.of("b67103865cff09c2638b8e8e8551175b18db2253.jpg");
		@Autowired
		ProductService productService;
		@Autowired
		UserRepository userRepository;
		@Autowired
		CacheManager cacheManager;

		@Nested
		class CreateCache {
			@Test
			@DisplayName("캐시 생성 - 상품 상세 조회")
			void t4() {

				Long productId = 1L;

				productService.findProductWithImgById(productId);

				Cache cache = cacheManager.getCache("product:detail");
				assertThat(cache.get(productId)).isNotNull();
			}
		}

		@Nested
		class DeleteCache {
			@Test
			@DisplayName("캐시 삭제 - 상품 생성")
			void t5() {
				User actor = userRepository.findById(1L).get();
				ProductCreateRequest productCreateRequest = new ProductCreateRequest(
					name,
					description,
					category,
					subCategory,
					images
				);

				Long productId = 1L;
				Cache cache = cacheManager.getCache("product:detail");

				productService.findProductWithImgById(productId);
				assertThat(cache.get(productId)).isNotNull();

				productService.addProduct(productCreateRequest, actor);
				assertThat(cache.get(productId)).isNull();
			}

			@Test
			@DisplayName("캐시 삭제 - 상품 수정")
			void t5_1() {
				User actor = userRepository.findById(1L).get();
				ProductCreateRequest productCreateRequest = new ProductCreateRequest(
					updatedName,
					description,
					category,
					subCategory,
					images
				);

				Long productId = 2L;
				Cache cache = cacheManager.getCache("product:detail");

				productService.findProductWithImgById(productId);
				assertThat(cache.get(productId)).isNotNull();

				productService.updateProduct(productId, productCreateRequest, actor);
				assertThat(cache.get(productId)).isNull();
			}

			@Test
			@DisplayName("캐시 삭제 - 상품 삭제")
			void t5_2() {
				User actor = userRepository.findById(1L).get();

				Long productId = 2L;
				Cache cache = cacheManager.getCache("product:detail");

				productService.findProductWithImgById(productId);
				assertThat(cache.get(productId)).isNotNull();

				productService.deleteProduct(productId, actor);
				assertThat(cache.get(productId)).isNull();
			}
		}
	}

}
