package org.com.drop.domain.auction.product.controller;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.com.drop.domain.auction.bid.dto.request.BidRequestDto;
import org.com.drop.domain.auction.bid.dto.response.BidHistoryResponse;
import org.com.drop.domain.auction.bid.dto.response.BidResponseDto;
import org.com.drop.domain.auction.bid.service.BidService;
import org.com.drop.domain.auction.product.dto.ProductCreateRequest;
import org.com.drop.domain.auction.product.entity.BookMark;
import org.com.drop.domain.auction.product.entity.Product;
import org.com.drop.domain.auction.product.entity.ProductImage;
import org.com.drop.domain.auction.product.repository.BookmarkRepository;
import org.com.drop.domain.auction.product.repository.ProductImageRepository;
import org.com.drop.domain.auction.product.repository.ProductRepository;
import org.com.drop.domain.auth.jwt.JwtProvider;
import org.com.drop.domain.user.controller.UserController;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.com.drop.global.aws.PreSignedUrlRequest;
import org.com.drop.global.exception.ErrorCode;
import org.com.drop.global.exception.ServiceException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@Disabled //aws 계정 있어야 하기 때문에 dev로 가는 코드에서는 disable 했습니다.
public class ProductControllerTest {

	private final Long productId = 2L;
	private final Long wrongProductId = Long.MAX_VALUE;
	private final Long auctionId = 2L;
	private final Long expirationAuctionId = 1L;
	private final String name = "테스트 상품명";
	private final String updatedName = "수정된 테스트 상품명";
	private final String description = "테스트 상품 상세 설명";
	private final Product.Category category = Product.Category.STARGOODS;
	private final Product.SubCategory subCategory = Product.SubCategory.ACC;
	private final List<String> images = List.of("b67103865cff09c2638b8e8e8551175b18db2253.jpg");
	private final List<String> wrongImages = List.of("img1.png", "img2.png");
	private final List<String> updatedImages = List.of("UpdatedImg1.png", "UpdatedImg2.png");
	private String jsonContent;

	@Autowired
	private MockMvc mvc;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private ProductRepository productRepository;
	@Autowired
	private ProductImageRepository productImageRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private BookmarkRepository bookmarkRepository;

	void setUp(String name, String description, Product.Category category, Product.SubCategory subCategory,
		List<String> images) throws JsonProcessingException {
		ProductCreateRequest testRequestDto = new ProductCreateRequest(
			name,
			description,
			category,
			subCategory,
			images
		);

		jsonContent = objectMapper.writeValueAsString(testRequestDto);
	}

	@Nested
	class ProductTest {
		@Nested
		class Read {
			@Test
			@DisplayName("상품 조회 - 성공")
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			void t1() throws Exception {
				ResultActions resultActions = mvc
					.perform(
						get("/api/v1/products/%d".formatted(productId))
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(UserController.class))
					.andExpect(handler().methodName("getProduct"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.code").value("SUCCESS"))
					.andExpect(jsonPath("$.message").value("요청을 성공적으로 처리했습니다."));

				Product product = productRepository.findById(productId).get();

				resultActions
					.andExpect(jsonPath("$.data.productId").value(product.getId()))
					.andExpect(jsonPath("$.data.sellerId").value(product.getSeller().getId()))
					.andExpect(jsonPath("$.data.name").value(product.getName()))
					.andExpect(jsonPath("$.data.description").value(product.getDescription()))
					.andExpect(jsonPath("$.data.category").value(product.getCategory().toString()))
					.andExpect(jsonPath("$.data.subCategory").value(product.getSubcategory().toString()))
					.andExpect(jsonPath("$.data.createdAt").isNotEmpty());

				List<ProductImage> productImages = productImageRepository.findAllByProductId(productId)
					.stream().sorted((a, b) -> a.getId().compareTo(b.getId()))
					.toList();
				for	(int i = 0; i < productImages.size(); i++ ) {
					assertThat(productImages.get(i).getImageUrl())
						.isEqualTo("b67103865cff09c2638b8e8e8551175b18db2253.jpg");
				}

			}

			@Test
			@DisplayName("상품 조회 - 실패 - 상품 없음")
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			void t1_1() throws Exception {
				ResultActions resultActions = mvc
					.perform(
						get("/api/v1/products/%d".formatted(wrongProductId))
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(UserController.class))
					.andExpect(handler().methodName("getProduct"))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
					.andExpect(jsonPath("$.httpStatus").value("404"))
					.andExpect(jsonPath("$.message").value("요청하신 상품 ID를 찾을 수 없습니다."));
			}

			@Test
			@DisplayName("상품 조회 - 실패 - 로그인 없음")
			void t1_2() throws Exception {
				ResultActions resultActions = mvc
					.perform(
						get("/api/v1/products/%d".formatted(productId))
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(UserController.class))
					.andExpect(handler().methodName("getProduct"))
					.andExpect(status().is(401))
					.andExpect(jsonPath("$.code").value("USER_UNAUTHORIZED"))
					.andExpect(jsonPath("$.httpStatus").value("401"))
					.andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
			}

			@Test
			@DisplayName("상품 조회 - 실패 - 계정 다름")
			@WithMockUser(username = "user2@example.com", roles = {"USER"})
			void t1_3() throws Exception {
				ResultActions resultActions = mvc
					.perform(
						get("/api/v1/products/%d".formatted(productId))
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(UserController.class))
					.andExpect(handler().methodName("getProduct"))
					.andExpect(status().is(403))
					.andExpect(jsonPath("$.code").value("USER_INACTIVE_USER"))
					.andExpect(jsonPath("$.httpStatus").value("403"))
					.andExpect(jsonPath("$.message").value("권한이 없는 사용자입니다"));
			}
		}

		@Nested
		class Exhibit {
			@Test
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			@DisplayName("상품 출품 - 성공")
			void t1() throws Exception {
				setUp(name, description, category, subCategory, images);

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
					.andExpect(jsonPath("$.code").value("SUCCESS"))
					.andExpect(jsonPath("$.message").value("요청을 성공적으로 처리했습니다."));

				resultActions
					.andExpect(jsonPath("$.data.productId").isNotEmpty())
					.andExpect(jsonPath("$.data.createdAt").isNotEmpty())
					.andExpect(jsonPath("$.data.updatedAt").isEmpty());

				List<ProductImage> productImages = productImageRepository.findAllByProductId(3L)
					.stream().sorted((a, b) -> a.getId().compareTo(b.getId()))
					.toList();
				for	(int i = 0; i < productImages.size(); i++ ) {
					assertThat(productImages.get(i).getImageUrl()).isEqualTo(images.get(i));
				}

			}

			@Test
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			@DisplayName("상품 출품 - 실패 - 필수값(이름) 누락")
			void t1_1() throws Exception {
				setUp("", description, category, subCategory, images);
				ResultActions resultActions = mvc
					.perform(
						post("/api/v1/products")
							.contentType(MediaType.APPLICATION_JSON)
							.content(jsonContent)
					)
					.andDo(print());

				resultActions
					.andExpect(status().isBadRequest());
			}

			@Test
			@DisplayName("상품 출품 - 실패 (로그인 없음)")
			void t1_2() throws Exception {
				setUp(name, description, category, subCategory, images);

				ResultActions resultActions = mvc
					.perform(
						post("/api/v1/products")
							.contentType(MediaType.APPLICATION_JSON)
							.content(jsonContent)
					).andDo(print());
				resultActions.andExpect(status().isForbidden());
			}

			@Test
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			@DisplayName("상품 출품 - 실패 (이미지 등록 없음)")
			void t1_3() throws Exception {
				setUp(name, description, category, subCategory, wrongImages);

				ResultActions resultActions = mvc
					.perform(
						post("/api/v1/products")
							.contentType(MediaType.APPLICATION_JSON)
							.content(jsonContent)
					).andDo(print());
				resultActions
					.andExpect(handler().handlerType(ProductController.class))
					.andExpect(handler().methodName("addProduct"))
					.andExpect(status().is(400))
					.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
					.andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
			}
		}

		@Nested
		class Update {
			@Test
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			@DisplayName("상품 수정 - 성공")
			void t2() throws Exception {
				setUp(updatedName, description, category, subCategory, images);
				ResultActions resultActions = mvc
					.perform(
						put("/api/v1/products/%d".formatted(auctionId))
							.contentType(MediaType.APPLICATION_JSON)
							.content(jsonContent)
							.with(csrf())
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(ProductController.class))
					.andExpect(handler().methodName("updateProduct"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.code").value("SUCCESS"))
					.andExpect(jsonPath("$.message").value("요청을 성공적으로 처리했습니다."));

				resultActions
					.andExpect(jsonPath("$.data.productId").isNotEmpty())
					.andExpect(jsonPath("$.data.createdAt").isNotEmpty())
					.andExpect(jsonPath("$.data.updatedAt").isNotEmpty());

				Product product = productRepository.findById(auctionId).get();
				assertThat(product.getName()).isEqualTo(updatedName);
			}

			@Test
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			@DisplayName("상품 수정 - 실패 (잘못된 상품 id)")
			void t2_1() throws Exception {
				setUp(updatedName, description, category, subCategory, images);
				ResultActions resultActions = mvc
					.perform(
						put("/api/v1/products/%d".formatted(wrongProductId))
							.contentType(MediaType.APPLICATION_JSON)
							.content(jsonContent)
							.with(csrf())
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(ProductController.class))
					.andExpect(handler().methodName("updateProduct"))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
					.andExpect(jsonPath("$.message").value("요청하신 상품 ID를 찾을 수 없습니다."));
			}

			@Test
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			@DisplayName("상품 수정 - 실패 (필수값(이름) 누락)")
			void t2_2() throws Exception {
				setUp("", description, category, subCategory, images);
				ResultActions resultActions = mvc
					.perform(
						put("/api/v1/products/%d".formatted(productId))
							.contentType(MediaType.APPLICATION_JSON)
							.content(jsonContent)
							.with(csrf())
					)
					.andDo(print());

				resultActions
					.andExpect(status().isBadRequest());
			}

			@Test
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			@DisplayName("상품 수정 - 실패 (경매 이미 시작)")
			void t2_3() throws Exception {
				setUp(updatedName, description, category, subCategory, images);
				try {
					Thread.sleep(6000); // 경매 만료되게 6초 대기
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				ResultActions resultActions = mvc
					.perform(
						put("/api/v1/products/%d".formatted(expirationAuctionId))
							.contentType(MediaType.APPLICATION_JSON)
							.content(jsonContent)
							.with(csrf())
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(ProductController.class))
					.andExpect(handler().methodName("updateProduct"))
					.andExpect(status().is(409))
					.andExpect(jsonPath("$.code").value("PRODUCT_ALREADY_ON_AUCTION"))
					.andExpect(jsonPath("$.message").value("이미 경매가 시작된 상품입니다."));
			}

			@Test
			@DisplayName("상품 수정 - 실패 (로그인 없음)")
			void t2_4() throws Exception {
				setUp(updatedName, description, category, subCategory, updatedImages);
				ResultActions resultActions = mvc
					.perform(
						put("/api/v1/products/%d".formatted(auctionId))
							.contentType(MediaType.APPLICATION_JSON)
							.content(jsonContent)
					)
					.andDo(print());
				resultActions.andExpect(status().isForbidden());
			}
		}

		@Nested
		class Delete {
			@Test
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			@DisplayName("상품 삭제 - 성공")
			void t3() throws Exception {
				ResultActions resultActions = mvc
					.perform(
						delete("/api/v1/products/%d".formatted(productId))
							.with(csrf())
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(ProductController.class))
					.andExpect(handler().methodName("deleteProduct"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.code").value("SUCCESS"))
					.andExpect(jsonPath("$.message").value("요청을 성공적으로 처리했습니다."));

				Optional<Product> product = productRepository.findById(productId);
				List<ProductImage> productImages = productImageRepository.findAllByProductId(productId);
				assertThat(product.get().getDeletedAt()).isNotNull();
				assertThat(productImages.size()).isEqualTo(0);
			}

			@Test
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			@DisplayName("상품 삭제 - 실패 (상품 없음)")
			void t3_1() throws Exception {
				ResultActions resultActions = mvc
					.perform(
						delete("/api/v1/products/%d".formatted(wrongProductId))
							.with(csrf())
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(ProductController.class))
					.andExpect(handler().methodName("deleteProduct"))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
					.andExpect(jsonPath("$.message").value("요청하신 상품 ID를 찾을 수 없습니다."));
			}

			@Test
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			@DisplayName("상품 삭제 - 실패 (경매 이미 시작)")
			void t3_2() throws Exception {
				try {
					Thread.sleep(6000); // 경매 만료되게 6초 대기
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				ResultActions resultActions = mvc
					.perform(
						delete("/api/v1/products/%d".formatted(expirationAuctionId))
							.with(csrf())
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(ProductController.class))
					.andExpect(handler().methodName("deleteProduct"))
					.andExpect(status().is(409))
					.andExpect(jsonPath("$.code").value("PRODUCT_ALREADY_ON_AUCTION"))
					.andExpect(jsonPath("$.message").value("이미 경매가 시작된 상품입니다."));
			}

			@Test
			@DisplayName("상품 삭제 - 실패 (로그인 없음)")
			void t1_2() throws Exception {
				ResultActions resultActions = mvc
					.perform(
						delete("/api/v1/products/%d".formatted(productId))
					)
					.andDo(print());
				resultActions.andExpect(status().isForbidden());
			}
		}
	}

	@Nested
	class BookmarkTest {
		@Nested
		class Create {
			@Test
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			@DisplayName("북마크 등록 - 성공")
			void t4() throws Exception {
				ResultActions resultActions = mvc
					.perform(
						post("/api/v1/products/%d/bookmarks".formatted(productId))
							.with(csrf())
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(ProductController.class))
					.andExpect(handler().methodName("addBookmark"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.code").value("SUCCESS"))
					.andExpect(jsonPath("$.status").value(200))
					.andExpect(jsonPath("$.message").value("요청을 성공적으로 처리했습니다."));

				resultActions
					.andExpect(jsonPath("$.data.bookmarkedId").isNotEmpty())
					.andExpect(jsonPath("$.data.productId").value(productId))
					.andExpect(jsonPath("$.data.bookmarkedAt").isNotEmpty());
			}

			@Test
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			@DisplayName("북마크 등록 - 실패 (상품 없음)")
			void t4_1() throws Exception {
				ResultActions resultActions = mvc
					.perform(
						post("/api/v1/products/%d/bookmarks".formatted(wrongProductId))
							.with(csrf())
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(ProductController.class))
					.andExpect(handler().methodName("addBookmark"))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
					.andExpect(jsonPath("$.httpStatus").value(404))
					.andExpect(jsonPath("$.message").value("요청하신 상품 ID를 찾을 수 없습니다."));
			}

			@Test
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			@DisplayName("북마크 등록 - 실패 (이미 등록)")
			void t4_2() throws Exception {
				ResultActions resultActions0 = mvc
					.perform(
						post("/api/v1/products/%d/bookmarks".formatted(productId))
					).andDo(print());
				ResultActions resultActions = mvc
					.perform(
						post("/api/v1/products/%d/bookmarks".formatted(productId))
							.with(csrf())
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(ProductController.class))
					.andExpect(handler().methodName("addBookmark"))
					.andExpect(status().is(409))
					.andExpect(jsonPath("$.code").value("PRODUCT_ALREADY_BOOKMARKED"))
					.andExpect(jsonPath("$.httpStatus").value(409))
					.andExpect(jsonPath("$.message").value("이미 찜한 상품입니다."));
			}

			@Test
			@DisplayName("북마크 등록 - 실패 (로그인 없음)")
			void t1_2() throws Exception {
				ResultActions resultActions = mvc
					.perform(
						post("/api/v1/products/%d/bookmarks".formatted(productId))
					)
					.andDo(print());
				resultActions.andExpect(status().isForbidden());
			}
		}

		@Nested
		class Delete {
			@Test
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			@DisplayName("북마크 삭제 - 성공")
			void t5() throws Exception {
				ResultActions resultActions0 = mvc
					.perform(
						post("/api/v1/products/%d/bookmarks".formatted(productId))
							.with(csrf())
					).andDo(print());
				ResultActions resultActions = mvc
					.perform(
						delete("/api/v1/products/%d/bookmarks".formatted(productId))
							.with(csrf())
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(ProductController.class))
					.andExpect(handler().methodName("deleteBookmark"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.code").value("SUCCESS"))
					.andExpect(jsonPath("$.status").value(200))
					.andExpect(jsonPath("$.message").value("요청을 성공적으로 처리했습니다."));

				Product product = productRepository.findById(productId).get();
				User user = userRepository.findById(1L).get();
				Optional<BookMark> bookMark = bookmarkRepository.findByProductAndUser(product, user);
				assertThat(bookMark.isPresent()).isFalse();
			}

			@Test
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			@DisplayName("북마크 삭제 - 실패 (상품 없음)")
			void t5_1() throws Exception {
				ResultActions resultActions = mvc
					.perform(
						delete("/api/v1/products/%d/bookmarks".formatted(wrongProductId))
							.with(csrf())
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(ProductController.class))
					.andExpect(handler().methodName("deleteBookmark"))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
					.andExpect(jsonPath("$.httpStatus").value(404))
					.andExpect(jsonPath("$.message").value("요청하신 상품 ID를 찾을 수 없습니다."));
			}

			@Test
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			@DisplayName("북마크 삭제 - 실패 (등록 이력 없음)")
			void t5_2() throws Exception {
				ResultActions resultActions = mvc
					.perform(
						delete("/api/v1/products/%d/bookmarks".formatted(productId))
							.with(csrf())
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(ProductController.class))
					.andExpect(handler().methodName("deleteBookmark"))
					.andExpect(status().is(404))
					.andExpect(jsonPath("$.code").value("USER_BOOKMARK_NOT_FOUND"))
					.andExpect(jsonPath("$.httpStatus").value(404))
					.andExpect(jsonPath("$.message").value("찜한 상품이 아닙니다."));
			}

			@Test
			@DisplayName("북마크 삭제 - 실패 (로그인 없음)")
			void t5_3() throws Exception {
				ResultActions resultActions = mvc
					.perform(
						delete("/api/v1/products/%d/bookmarks".formatted(productId))
					)
					.andDo(print());
				resultActions.andExpect(status().isForbidden());
			}

		}
	}

	@Nested
	class PreSigned {
		@Nested
		class MakeUrl {
			@Test
			@WithMockUser(username = "user1@example.com", roles = {"USER"})
			@DisplayName("Url 발급 - 성공")
			void t6() throws Exception {
				List<PreSignedUrlRequest> requests = List.of(
					new PreSignedUrlRequest("image/jpeg", 1024L),
					new PreSignedUrlRequest("image/png", 2048L)
				);

				String jsonContent = objectMapper.writeValueAsString(requests);

				ResultActions resultActions = mvc
					.perform(
						post("/api/v1/products/img")
							.contentType(MediaType.APPLICATION_JSON)
							.content(jsonContent)
							.with(csrf())
					)
					.andDo(print());

				resultActions
					.andExpect(handler().handlerType(ProductController.class))
					.andExpect(handler().methodName("getImageUrl"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.code").value("SUCCESS"))
					.andExpect(jsonPath("$.status").value("200"))
					.andExpect(jsonPath("$.message").value("요청을 성공적으로 처리했습니다."))
					.andExpect(jsonPath("$.data").isArray())
					.andExpect(jsonPath("$.data.length()").value(2))
					.andExpect(jsonPath("$.data[0]").value(containsString(".jpg")))
					.andExpect(jsonPath("$.data[1]").value(containsString(".png")));
			}

			@Test
			@DisplayName("Url 발급 - 실패 (로그인 없음)")
			void t6_1() throws Exception {
				ResultActions resultActions = mvc
					.perform(
						get("/api/v1/products/img")
					)
					.andDo(print());
				resultActions.andExpect(status().isUnauthorized());
			}
		}
	}

	@Nested
	class Bid{
		@Autowired
		MockMvc mockMvc;

		@MockitoBean
		JwtProvider jwtProvider;

		@MockitoBean
		BidService bidService;

		@DisplayName("입찰요청이 오면 200과 BidResponseDto를 반환한다")
		@Test
		void returns200AndBidResponseDtoWhenBidRequested() throws Exception {
			//given
			Long auctionId = 12345L;
			Long userId = 987L;

			LocalDateTime bidTime = LocalDateTime.of(2025, 12, 5, 15, 50);

			BidResponseDto serviceResponse = BidResponseDto.of(
				auctionId,
				true,
				11_000L,
				bidTime
			);

			given(bidService.placeBid(eq(auctionId), eq(userId), any(BidRequestDto.class)))
				.willReturn(serviceResponse);

			//when&then
			mockMvc.perform(
					post("/bids/{auctionId}/bids", auctionId)
						.queryParam("userId", String.valueOf(userId))
						.contentType(String.valueOf(MediaType.APPLICATION_JSON))
						.content("""
						{ "bidAmount": 11000 }
						""")
				)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.auctionId").value(12345))
				.andExpect(jsonPath("$.data.isHighestBidder").value(true))
				.andExpect(jsonPath("$.data.currentHighestBid").value(11000))
				.andExpect(jsonPath("$.data.bidTime").value("2025-12-05T15:50:00"));
		}

		@DisplayName("최소입찰단위 미만이면 400으로 에러응답한다")
		@Test
		void returns400WhenBidAmountBelowBidUnit() throws Exception {
			// given
			Long auctionId = 12345L;
			Long userId = 987L;
			// 서비스에서 예외 던지도록 설정
			given(bidService.placeBid(auctionId, userId, new BidRequestDto(10000L)))
				.willThrow(new ServiceException(ErrorCode.AUCTION_BID_AMOUNT_TOO_LOW,
					"입찰 금액이 현재 최고가보다 낮거나 최소 입찰 단위를 충족하지 못했습니다."));

			// when & then
			mockMvc.perform(
					post("/bids/{auctionId}/bids", auctionId)
						.queryParam("userId", String.valueOf(userId))
						.contentType(String.valueOf(MediaType.APPLICATION_JSON))
						.content("""
							{ "bidAmount": 10000 }
						""")
				)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.httpStatus").value(400))
				.andExpect(jsonPath("$.code").value("AUCTION_BID_AMOUNT_TOO_LOW"))
				.andExpect(jsonPath("$.message").value("입찰 금액이 현재 최고가보다 낮거나 최소 입찰 단위를 충족하지 못했습니다."))
				.andDo(print());
		}

		@Test
		@DisplayName("입찰 리스트 페이지 요청시 200과 페이지 리스트 형태로 반환한다")
		void canRetrieveBidHistoryAsPagedList() throws Exception {
			// given
			Long auctionId = 1L;

			// 테스트용 응답 데이터 생성 (Builder 사용)
			BidHistoryResponse bid1 = BidHistoryResponse.builder()
				.bidId(100L)
				.bidAmount(50000L)
				.bidder("te***") // 이미 마스킹된 상태라고 가정
				.bidTime(LocalDateTime.now())
				.build();

			BidHistoryResponse bid2 = BidHistoryResponse.builder()
				.bidId(99L)
				.bidAmount(48000L)
				.bidder("us***")
				.bidTime(LocalDateTime.now().minusMinutes(10))
				.build();

			// Service가 리턴할 Page 객체 Mocking
			List<BidHistoryResponse> responseList = List.of(bid1, bid2);
			Page<BidHistoryResponse> pageResult = new PageImpl<>(responseList);

			// Service 메서드 호출 시, 위에서 만든 pageResult를 리턴하도록 설정
			given(bidService.getBidHistory(eq(auctionId), any(Pageable.class)))
				.willReturn(pageResult);

			// when
			ResultActions resultActions = mockMvc.perform(
				get("/api/bids/{auctionId}", auctionId)
					.param("page", "0")
					.param("size", "10")
					.param("sort", "createdAt,desc") // 요청 파라미터 시뮬레이션
					.contentType(String.valueOf(MediaType.APPLICATION_JSON))
					.with(csrf()) // 시큐리티 설정이 있다면 CSRF 토큰 필요
			);

			// then
			resultActions
				.andExpect(status().isOk())
				.andDo(print())
				.andExpect(jsonPath("$.data.content[0].bidId").value(100L))
				.andExpect(jsonPath("$.data.content[0].bidder").value("te***"))
				.andExpect(jsonPath("$.data.content[0].bidAmount").value(50000L))
				.andExpect(jsonPath("$.data.content[1].bidId").value(99L))
				// 페이징 메타데이터 검증
				.andExpect(jsonPath("$.data.size").value(2))      // 현재 페이지의 데이터 수 (PageImpl 기본 동작)
				.andExpect(jsonPath("$.data.totalElements").value(2));
		}

	}

}
