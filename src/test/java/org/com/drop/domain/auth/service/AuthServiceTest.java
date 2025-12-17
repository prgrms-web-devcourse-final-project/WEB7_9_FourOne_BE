package org.com.drop.domain.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.com.drop.domain.auth.dto.LocalLoginRequest;
import org.com.drop.domain.auth.dto.LocalLoginResponse;
import org.com.drop.domain.auth.dto.LocalSignUpRequest;
import org.com.drop.domain.auth.dto.LocalSignUpResponse;
import org.com.drop.domain.auth.dto.TokenRefreshResponse;
import org.com.drop.domain.auth.dto.UserDeleteRequest;
import org.com.drop.domain.auth.dto.UserDeleteResponse;
import org.com.drop.domain.auth.email.service.EmailService;
import org.com.drop.domain.auth.jwt.JwtProvider;
import org.com.drop.domain.auth.store.RefreshTokenStore;
import org.com.drop.domain.auth.store.VerificationCodeStore;
import org.com.drop.domain.user.dto.GetCurrentUserInfoResponse;
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

	@Mock
	private EmailService emailService;

	@Mock
	private VerificationCodeStore verificationCodeStore;

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
			when(verificationCodeStore.isVerified(anyString())).thenReturn(true);

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
			verify(verificationCodeStore, times(1)).isVerified(validRequest.email());
			verify(verificationCodeStore, times(1)).removeVerifiedMark(validRequest.email());
			verify(userRepository, times(1)).existsByEmail(validRequest.email());
			verify(userRepository, times(1)).existsByNickname(validRequest.nickname());
			verify(userRepository, times(1)).save(any(User.class));
		}

		@Test
		@DisplayName("실패: 이메일 인증 미완료")
		void signup_fail_notVerified() {
			// Given
			when(verificationCodeStore.isVerified(anyString())).thenReturn(false);

			// When & Then
			ServiceException exception = assertThrows(ServiceException.class,
				() -> authService.signup(validRequest)
			);
			assertEquals(ErrorCode.AUTH_CODE_EXPIRED, exception.getErrorCode());

			// Verify
			verify(verificationCodeStore, times(1)).isVerified(validRequest.email());
			verify(userRepository, never()).existsByEmail(anyString());
			verify(userRepository, never()).save(any(User.class));
		}

		@Test
		@DisplayName("실패: 이메일 중복 시 ServiceException 발생")
		void signup_fail_duplicateEmail() {
			// Given
			when(verificationCodeStore.isVerified(anyString())).thenReturn(true);

			when(userRepository.existsByEmail(anyString())).thenReturn(true);

			// When & Then
			ServiceException exception = assertThrows(ServiceException.class,
				() -> authService.signup(validRequest)
			);

			assertEquals(ErrorCode.AUTH_DUPLICATE_EMAIL, exception.getErrorCode());

			// Verify
			verify(verificationCodeStore, times(1)).isVerified(anyString());
			verify(userRepository, times(1)).existsByEmail(anyString());
			verify(userRepository, never()).save(any(User.class));
		}

		@Test
		@DisplayName("실패: 닉네임 중복 시 ServiceException 발생")
		void signup_fail_duplicateNickname() {
			// Given
			when(verificationCodeStore.isVerified(anyString())).thenReturn(true);

			when(userRepository.existsByEmail(anyString())).thenReturn(false);
			when(userRepository.existsByNickname(anyString())).thenReturn(true);

			// When & Then
			ServiceException exception = assertThrows(ServiceException.class,
				() -> authService.signup(validRequest)
			);

			assertEquals(ErrorCode.AUTH_DUPLICATE_NICKNAME, exception.getErrorCode());

			verify(verificationCodeStore, times(1)).isVerified(anyString());
			verify(userRepository, times(1)).existsByEmail(anyString());
			verify(userRepository, times(1)).existsByNickname(anyString());
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

	@Nested
	@DisplayName("인증 코드 발송")
	class SendVerificationCodeTests {
		private final String testEmail = "send_test@drop.com";

		@Test
		@DisplayName("성공: 코드 생성, Redis 저장, 이메일 발송 검증")
		void sendCode_success() {
			// Given
			when(userRepository.existsByEmail(testEmail)).thenReturn(false);

			// When
			authService.sendVerificationCode(testEmail);

			// Then
			verify(userRepository, times(1)).existsByEmail(testEmail);
			verify(verificationCodeStore, times(1)).saveCode(eq(testEmail), anyString());
			verify(emailService, times(1)).sendVerificationEmail(eq(testEmail), anyString());
		}

		@Test
		@DisplayName("실패: 이미 등록된 이메일")
		void sendCode_fail_duplicateEmail() {
			// Given
			when(userRepository.existsByEmail(testEmail)).thenReturn(true);

			// When & Then
			ServiceException exception = assertThrows(ServiceException.class,
				() -> authService.sendVerificationCode(testEmail)
			);
			assertEquals(ErrorCode.AUTH_DUPLICATE_EMAIL, exception.getErrorCode());

			// Verify
			verify(verificationCodeStore, never()).saveCode(anyString(), anyString());
			verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
		}

		@Test
		@DisplayName("실패: Redis 저장 실패 시 예외 변환")
		void sendCode_fail_redisError() {
			// Given
			when(userRepository.existsByEmail(testEmail)).thenReturn(false);
			doThrow(new RuntimeException("Redis connection error"))
				.when(verificationCodeStore).saveCode(eq(testEmail), anyString());

			// When & Then
			ServiceException exception = assertThrows(ServiceException.class,
				() -> authService.sendVerificationCode(testEmail)
			);
			assertEquals(ErrorCode.AUTH_EMAIL_SEND_FAILED, exception.getErrorCode());

			// Verify
			verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
		}
	}

	@Nested
	@DisplayName("인증 코드 검증")
	class VerifyCodeTests {
		private final String testEmail = "verify_test@drop.com";
		private final String validCode = "123456";
		private final String wrongCode = "000000";

		@Test
		@DisplayName("성공: 코드 일치 시 Redis 코드 삭제 및 Verified 마크")
		void verifyCode_success() {
			// Given
			when(verificationCodeStore.getCode(testEmail)).thenReturn(validCode);

			// When
			authService.verifyCode(testEmail, validCode);

			// Then
			verify(verificationCodeStore, times(1)).removeCode(testEmail);
			verify(verificationCodeStore, times(1)).markAsVerified(testEmail);
		}

		@Test
		@DisplayName("실패: 코드가 만료되었거나 존재하지 않음")
		void verifyCode_fail_codeExpired() {
			// Given
			when(verificationCodeStore.getCode(testEmail)).thenReturn(null); // 만료/부재

			// When & Then
			ServiceException exception = assertThrows(ServiceException.class,
				() -> authService.verifyCode(testEmail, validCode)
			);
			assertEquals(ErrorCode.AUTH_CODE_EXPIRED, exception.getErrorCode());

			// Verify
			verify(verificationCodeStore, never()).removeCode(anyString());
			verify(verificationCodeStore, never()).markAsVerified(anyString());
		}

		@Test
		@DisplayName("실패: 코드가 일치하지 않음")
		void verifyCode_fail_codeMismatch() {
			// Given
			when(verificationCodeStore.getCode(testEmail)).thenReturn(validCode);

			// When & Then
			ServiceException exception = assertThrows(ServiceException.class,
				() -> authService.verifyCode(testEmail, wrongCode)
			);
			assertEquals(ErrorCode.AUTH_CODE_MISMATCH, exception.getErrorCode());

			// Verify
			verify(verificationCodeStore, never()).removeCode(anyString());
			verify(verificationCodeStore, never()).markAsVerified(anyString());
		}
	}
}
