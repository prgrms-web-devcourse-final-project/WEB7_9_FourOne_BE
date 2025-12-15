package org.com.drop.domain.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.com.drop.domain.auth.dto.GetCurrentUserInfoResponse;
import org.com.drop.domain.auth.dto.LocalLoginRequest;
import org.com.drop.domain.auth.dto.LocalLoginResponse;
import org.com.drop.domain.auth.dto.LocalSignUpRequest;
import org.com.drop.domain.auth.dto.LocalSignUpResponse;
import org.com.drop.domain.auth.dto.TokenRefreshResponse;
import org.com.drop.domain.auth.dto.UserDeleteRequest;
import org.com.drop.domain.auth.dto.UserDeleteResponse;
import org.com.drop.domain.auth.jwt.JwtProvider;
import org.com.drop.domain.auth.store.RefreshTokenStore;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.com.drop.global.exception.ErrorCode;
import org.com.drop.global.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

	@InjectMocks
	private AuthService authService;

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private RefreshTokenStore refreshTokenStore;

	@Mock
	private JwtProvider jwtProvider;

	@Mock
	private SecurityContext securityContext;

	@Mock
	private Authentication authentication;

	private User mockUser;

	@BeforeEach
	void setUp() {
		mockUser = User.builder()
			.id(1L)
			.email("test@drop.com")
			.password("encoded_password")
			.nickname("testuser")
			.createdAt(LocalDateTime.now())
			.build();

		SecurityContextHolder.setContext(securityContext);
	}

	@Test
	@DisplayName("현재 사용자 정보 조회")
	void getMe_success() {
		// Given (mockUser 사용)

		// When
		GetCurrentUserInfoResponse response = authService.getMe(mockUser);

		// Then
		assertNotNull(response);
		assertEquals(mockUser.getId(), response.userId());
		assertEquals(mockUser.getEmail(), response.email());
		assertEquals(mockUser.getNickname(), response.nickname());
	}

	@Nested
	@DisplayName("회원가입")
	class SignupTests {
		private LocalSignUpRequest validRequest;

		@BeforeEach
		void setup() {
			validRequest = new LocalSignUpRequest("new@drop.com", "password123", "newuser");
		}

		@Test
		@DisplayName("성공: 유효한 정보로 회원가입")
		void signup_success() {
			// Given
			when(userRepository.existsByEmail(anyString())).thenReturn(false);
			when(userRepository.existsByNickname(anyString())).thenReturn(false);
			when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
			when(userRepository.save(any(User.class))).thenReturn(mockUser);

			// When
			LocalSignUpResponse response = authService.signup(validRequest);

			// Then
			assertNotNull(response);
			assertEquals(mockUser.getEmail(), response.email());
			assertEquals(mockUser.getNickname(), response.nickname());

			// Verify
			verify(userRepository, times(1)).existsByEmail(validRequest.email());
			verify(userRepository, times(1)).existsByNickname(validRequest.nickname());
			verify(userRepository, times(1)).save(any(User.class));
		}

		@Test
		@DisplayName("실패: 이메일 중복 시 ServiceException 발생")
		void signup_fail_duplicateEmail() {
			// Given
			when(userRepository.existsByEmail(anyString())).thenReturn(true);

			// When & Then
			ServiceException exception = assertThrows(ServiceException.class,
				() -> authService.signup(validRequest)
			);
			assertEquals(ErrorCode.AUTH_DUPLICATE_EMAIL, exception.getErrorCode());

			// Verify
			verify(userRepository, never()).save(any(User.class));
		}

		@Test
		@DisplayName("실패: 닉네임 중복 시 ServiceException 발생")
		void signup_fail_duplicateNickname() {
			// Given
			when(userRepository.existsByEmail(anyString())).thenReturn(false);
			when(userRepository.existsByNickname(anyString())).thenReturn(true);

			// When & Then
			ServiceException exception = assertThrows(ServiceException.class,
				() -> authService.signup(validRequest)
			);
			assertEquals(ErrorCode.AUTH_DUPLICATE_NICKNAME, exception.getErrorCode());
		}
	}

	@Nested
	@DisplayName("로그인")
	class LoginTests {
		private LocalLoginRequest validRequest;

		@BeforeEach
		void setup() {
			validRequest = new LocalLoginRequest("test@drop.com", "valid_password");
		}

		@Test
		@DisplayName("성공: 유효한 인증 정보로 로그인")
		void login_success() {
			// Given
			when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
			when(passwordEncoder.matches(eq(validRequest.password()), eq(mockUser.getPassword()))).thenReturn(true);
			when(jwtProvider.createAccessToken(anyString())).thenReturn("mocked.access.token");
			when(jwtProvider.getAccessTokenValidityInSeconds()).thenReturn(3600L);

			// When
			LocalLoginResponse response = authService.login(validRequest);

			// Then
			assertNotNull(response);
			assertEquals(mockUser.getEmail(), response.email());
			assertEquals("mocked.access.token", response.accessToken());
			assertEquals(3600L, response.expiresIn());

			// Verify
			verify(jwtProvider, times(1)).createAccessToken(mockUser.getEmail());
			verify(jwtProvider, times(1)).getAccessTokenValidityInSeconds();
		}

		@Test
		@DisplayName("실패: 등록되지 않은 이메일")
		void login_fail_userNotFound() {
			// Given
			when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

			// When & Then
			ServiceException exception = assertThrows(ServiceException.class,
				() -> authService.login(validRequest)
			);
			assertEquals(ErrorCode.AUTH_UNAUTHORIZED, exception.getErrorCode());
		}

		@Test
		@DisplayName("실패: 비밀번호 불일치")
		void login_fail_passwordMismatch() {
			// Given
			when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
			when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

			// When & Then
			ServiceException exception = assertThrows(ServiceException.class,
				() -> authService.login(validRequest)
			);
			assertEquals(ErrorCode.AUTH_UNAUTHORIZED, exception.getErrorCode());

			// Verify
			verify(jwtProvider, never()).createAccessToken(anyString());
		}
	}

	@Nested
	@DisplayName("계정 삭제")
	class DeleteAccountTests {
		private UserDeleteRequest validRequest;

		@BeforeEach
		void setup() {
			validRequest = new UserDeleteRequest("valid_password");
		}

		@Test
		@DisplayName("성공: 유효한 정보로 계정 삭제")
		void deleteAccount_success() {
			// Given
			when(passwordEncoder.matches(eq(validRequest.password()), eq(mockUser.getPassword()))).thenReturn(true);
			when(userRepository.save(any(User.class))).thenReturn(mockUser);

			// When
			UserDeleteResponse response = authService.deleteAccount(mockUser, validRequest);

			// Then
			assertNotNull(response);
			assertNotNull(mockUser.getDeletedAt()); // DeletedAt 필드가 설정되었는지 확인

			// Verify
			verify(userRepository, times(1)).save(mockUser);
			verify(refreshTokenStore, times(1)).delete(mockUser.getEmail());
		}

		@Test
		@DisplayName("실패: 비밀번호 불일치")
		void deleteAccount_fail_passwordMismatch() {
			// Given
			when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

			// When & Then
			ServiceException exception = assertThrows(ServiceException.class,
				() -> authService.deleteAccount(mockUser, validRequest)
			);
			assertEquals(ErrorCode.AUTH_PASSWORD_MISMATCH, exception.getErrorCode());

			// Verify
			verify(userRepository, never()).save(any(User.class));
			verify(refreshTokenStore, never()).delete(anyString());
		}

		// Todo: 진행 중인 경매가 있을 경우 실패 테스트
	}

	@Nested
	@DisplayName("토큰 재발급")
	class RefreshTests {
		private final String validRefreshToken = "mocked.refresh.token";

		@Test
		@DisplayName("성공: 유효한 Refresh Token으로 Access Token 재발급")
		void refresh_success() {
			// Given
			when(jwtProvider.getUsername(validRefreshToken)).thenReturn(mockUser.getEmail());
			when(refreshTokenStore.exists(mockUser.getEmail(), validRefreshToken)).thenReturn(true);
			when(jwtProvider.createAccessToken(mockUser.getEmail())).thenReturn("new.access.token");
			when(jwtProvider.getAccessTokenValidityInSeconds()).thenReturn(1800L);

			// When
			TokenRefreshResponse response = authService.refresh(validRefreshToken);

			// Then
			assertNotNull(response);
			assertEquals("new.access.token", response.accessToken());
			assertEquals(1800L, response.expiresIn());

			// Verify
			verify(refreshTokenStore, times(1)).exists(mockUser.getEmail(), validRefreshToken);
		}

		@Test
		@DisplayName("실패: Refresh Token 파싱 오류")
		void refresh_fail_invalidToken() {
			// Given
			when(jwtProvider.getUsername(validRefreshToken)).thenThrow(new RuntimeException("JWT parsing failed"));

			// When & Then
			ServiceException exception = assertThrows(ServiceException.class,
				() -> authService.refresh(validRefreshToken)
			);
			assertEquals(ErrorCode.AUTH_TOKEN_INVALID, exception.getErrorCode());

			// Verify
			verify(refreshTokenStore, never()).exists(anyString(), anyString());
			verify(jwtProvider, never()).createAccessToken(anyString());
		}

		@Test
		@DisplayName("실패: Redis에 Refresh Token이 존재하지 않음")
		void refresh_fail_tokenNotExistsInStore() {
			// Given
			when(jwtProvider.getUsername(validRefreshToken)).thenReturn(mockUser.getEmail());
			when(refreshTokenStore.exists(mockUser.getEmail(), validRefreshToken)).thenReturn(false);

			// When & Then
			ServiceException exception = assertThrows(ServiceException.class,
				() -> authService.refresh(validRefreshToken)
			);
			assertEquals(ErrorCode.AUTH_TOKEN_INVALID, exception.getErrorCode());
		}
	}
}
