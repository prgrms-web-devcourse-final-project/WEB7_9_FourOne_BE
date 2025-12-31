package org.com.drop.domain.user.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.com.drop.domain.auction.product.service.ProductService;
import org.com.drop.domain.user.dto.MyPageResponse;
import org.com.drop.domain.user.dto.UpdateProfileRequest;
import org.com.drop.domain.user.dto.UpdateProfileResponse;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.service.UserService;
import org.com.drop.global.aws.AmazonS3Client;
import org.com.drop.global.aws.ImageType;
import org.com.drop.global.aws.PreSignedUrlListRequest;
import org.com.drop.global.aws.PreSignedUrlRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("UserController 통합 테스트")
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private ProductService productService;

	@MockitoBean
	private AmazonS3Client amazonS3Client;

	@MockitoBean
	private StringRedisTemplate stringRedisTemplate;

	@MockitoBean
	ApplicationRunner baseInitData;

	private User mockActor;

	@BeforeEach
	void setUp() {
		mockActor = User.builder()
			.id(1L)
			.email("test@drop.com")
			.nickname("테스트유저")
			.build();
	}

	@Test
	@WithMockUser(username = "test@drop.com")
	@DisplayName("POST /api/v1/user/me/profile/img - 프로필 이미지 Presigned URL 생성")
	void getProfileImageUrl_success() throws Exception {

		// Given
		List<String> expectedUrls = List.of("https://s3.url/1");

		given(userService.findUserByEmail("test@drop.com"))
			.willReturn(mockActor);

		given(amazonS3Client.createPresignedUrls(
			any(PreSignedUrlListRequest.class),
			eq(mockActor),
			eq(ImageType.PROFILE)
		)).willReturn(expectedUrls);


		PreSignedUrlListRequest request = new PreSignedUrlListRequest(
			List.of(new PreSignedUrlRequest("image/jpeg", 20L))
		);

		// When & Then
		mockMvc.perform(post("/api/v1/user/me/profile/img")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0]").value("https://s3.url/1"))
			.andDo(print());
	}

	@Nested
	@DisplayName("내 정보 관련 API")
	class MyInfoTests {

		@Test
		@WithMockUser
		@DisplayName("GET /api/v1/user/me - 내 프로필 정보 조회")
		void getMe_success() throws Exception {
			// Given
			MyPageResponse response = MyPageResponse.of(mockActor, "https://example.com/profile.jpg");
			given(userService.getMe(any())).willReturn(response);

			// When & Then
			mockMvc.perform(get("/api/v1/user/me"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.userId").value(mockActor.getId()))
				.andExpect(jsonPath("$.data.nickname").value(mockActor.getNickname()))
				.andDo(print());
		}

		@Test
		@WithMockUser
		@DisplayName("PATCH /api/v1/user/me/profile - 프로필 수정")
		void updateProfile_success() throws Exception {
			// Given
			UpdateProfileRequest request = new UpdateProfileRequest("새닉네임", "new_image.jpg");

			mockActor.updateProfile("새닉네임", "new_image.jpg");
			UpdateProfileResponse response =
				UpdateProfileResponse.of(mockActor, null);

			given(userService.updateProfile(any(), any())).willReturn(response);

			// When & Then
			mockMvc.perform(patch("/api/v1/user/me/profile")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.nickname").value("새닉네임"))
				.andDo(print());
		}
	}

	@Nested
	@DisplayName("조회 및 목록 API")
	class SearchAndListTests {

		@Test
		@WithMockUser
		@DisplayName("GET /api/v1/me/products - 내 등록 상품 목록 조회")
		void getMyProducts_success() throws Exception {
			// Given
			int page = 1;

			// When & Then
			mockMvc.perform(get("/api/v1/me/products")
					.param("page", String.valueOf(page)))
				.andExpect(status().isOk())
				.andDo(print());

			verify(userService).getMyProducts(any(), eq(page));
		}

		@Test
		@WithMockUser
		@DisplayName("GET /api/v1/products/{productId} - 특정 상품 상세 조회")
		void getProduct_success() throws Exception {
			// Given
			Long productId = 10L;

			// When & Then
			mockMvc.perform(get("/api/v1/products/{productId}", productId))
				.andExpect(status().isOk())
				.andDo(print());

			verify(productService).findProductWithImgById(eq(productId), any());
		}
	}
}
