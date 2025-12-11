package org.com.drop.domain.auth.controller;

import org.com.drop.domain.auth.dto.LocalSignUpRequest;
import org.com.drop.domain.auth.dto.LocalSignUpResponse;
import org.com.drop.domain.auth.dto.UserDeleteRequest;
import org.com.drop.domain.auth.dto.UserDeleteResponse;
import org.com.drop.domain.auth.service.AuthService;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.com.drop.global.exception.ErrorCode;
import org.com.drop.global.exception.ServiceException;
import org.com.drop.global.rsData.RsData;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final UserRepository userRepository;

	@PostMapping("/local/signup")
	public ResponseEntity<RsData<LocalSignUpResponse>> signup(
		@Valid @RequestBody LocalSignUpRequest dto) {

		LocalSignUpResponse response = authService.signup(dto);

		RsData<LocalSignUpResponse> rsData = new RsData<>(
			"SUCCESS",
			"200",
			"요청을 성공적으로 처리했습니다.",
			response
		);

		return ResponseEntity.ok(rsData);
	}

	@PostMapping("/delete")
	public RsData<UserDeleteResponse> deleteAccount(
		@AuthenticationPrincipal UserDetails userDetails,
		@Validated @RequestBody UserDeleteRequest request) {

		User user = findUserFromUserDetails(userDetails);

		UserDeleteResponse response = authService.deleteAccount(user, request);

		return new RsData<>(
			"SUCCESS",
			"200",
			"요청을 성공적으로 처리했습니다.",
			response
		);
	}

	private User findUserFromUserDetails(UserDetails userDetails) {
		return userRepository.findByEmail(userDetails.getUsername())
			.orElseThrow(() -> new ServiceException(ErrorCode.USER_NOT_FOUND));
	}
}
