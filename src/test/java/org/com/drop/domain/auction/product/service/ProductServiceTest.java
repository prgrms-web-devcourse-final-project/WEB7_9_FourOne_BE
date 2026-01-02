package org.com.drop.domain.auction.product.service;

import static org.assertj.core.api.AssertionsForClassTypes.*;

import java.util.List;

import org.com.drop.domain.auction.product.dto.ProductCreateRequest;
import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.auction.product.repository.ProductImageRepository;
import org.com.drop.domain.auction.product.repository.ProductRepository;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.com.drop.global.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
@Disabled
public class ProductServiceTest {
	private final String name = "테스트 상품명";
	private final String updatedName = "수정된 테스트 상품명";
	private final String description = "테스트 상품 상세 설명";
	private final Product.Category category = Product.Category.STARGOODS;
	private final Product.SubCategory subCategory = Product.SubCategory.ACC;
	private final List<String> images = List.of("b67103865cff09c2638b8e8e8551175b18db2253.jpg");
	@Autowired
	ProductService productService;
	@Autowired
	ProductRepository productRepository;
	@Autowired
	ProductImageRepository productImageRepository;
	@Autowired
	UserRepository userRepository;
	@Autowired
	CacheManager cacheManager;
	private User testUser;
	private String wrongImageUrl = "잘못된 이미지 url";

	@BeforeEach
	void setUp() {
		testUser = userRepository.findById(1L).get();
	}


	@Nested
	class TransactionalTests {



		@Nested
		class AddProduct {
			@Test
			@DisplayName("상품 생성 실패 - 상품 저장 성공, 이미지 저장 실패")
			void t1() {
				List<String> invalidUrls = List.of("invalid-url");
				ProductCreateRequest request = new ProductCreateRequest(
					updatedName, "설명", Product.Category.STARGOODS, Product.SubCategory.ACC, invalidUrls);

				assertThatThrownBy(() -> productService.addProduct(request, testUser))
					.isInstanceOf(RuntimeException.class);
				assertThat(productRepository.findByName(updatedName).size()).isEqualTo(0);
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
					updatedName, "설명", Product.Category.STARGOODS, Product.SubCategory.ACC, invalidUrls);

				assertThatThrownBy(() -> productService.updateProduct(1L, request, testUser))
					.isInstanceOf(RuntimeException.class);

				assertThat(product.getName()).isNotEqualTo(updatedName);
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

				productService.updateProduct(productId, productCreateRequest, testUser);
				assertThat(cache.get(productId)).isNull();
			}
		}
	}

	@Nested
	@Transactional
	class validAuctionTest {
		@Test
		@DisplayName("상품 검증 - 경매 시작 상품")
		void t6() throws InterruptedException {
			Product product = productService.findProductById(1L);
			Thread.sleep(5000);
			assertThatThrownBy(() -> productService.validAuction(product))
				.isInstanceOf(ServiceException.class);
		}

		@Test
		@DisplayName("상품 수정 - 경매 시작 상품")
		void t6_1() throws InterruptedException {
			ProductCreateRequest productCreateRequest =
				new ProductCreateRequest(name, description, category, subCategory, images);
			Thread.sleep(5000);
			assertThatThrownBy(() -> productService.updateProduct(1L, productCreateRequest, testUser))
				.isInstanceOf(ServiceException.class);
		}

		@Test
		@DisplayName("상품 삭제 - 경매 시작 상품")
		void t6_2() throws InterruptedException {
			Thread.sleep(5000);
			assertThatThrownBy(() -> productService.deleteProduct(1L, testUser))
				.isInstanceOf(ServiceException.class);
		}

		@Test
		@DisplayName("상품 검증 - 경매 이전 상품")
		void t7() {
			Product product = productService.findProductById(2L);
			productService.validAuction(product);
		}

		@Test
		@DisplayName("상품 수정 - 경매 이전 상품")
		void t7_1() {
			ProductCreateRequest productCreateRequest =
				new ProductCreateRequest(updatedName, description, category, subCategory, images);
			Product product = productService.updateProduct(2L, productCreateRequest, testUser);
			Product product2 = productService.findProductById(2L);
			assertThat(product.getName()).isEqualTo(product2.getName());
			assertThat(product.getDescription()).isEqualTo(product2.getDescription());
			assertThat(product.getCategory()).isEqualTo(product2.getCategory());
			assertThat(product.getSubcategory()).isEqualTo(product2.getSubcategory());
			assertThat(product.getSeller().getId()).isEqualTo(product2.getSeller().getId());
			assertThat(product.getCreatedAt()).isEqualTo(product2.getCreatedAt());
			assertThat(product.getUpdatedAt()).isEqualTo(product2.getUpdatedAt());
		}

		@Test
		@DisplayName("상품 삭제 - 경매 이전 상품")
		void t7_2() {
			productService.deleteProduct(2L, testUser);
			assertThatThrownBy(() -> productService.findProductById(2L))
				.isInstanceOf(ServiceException.class);
		}
	}
}
