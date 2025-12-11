package org.com.drop.domain.auth.controller;

import org.com.drop.domain.auth.dto.LocalSignUpRequest;
import org.com.drop.domain.auth.dto.LocalSignUpResponse;
import org.com.drop.domain.auth.service.AuthService;
import org.com.drop.global.rsData.RsData;
import org.springframework.http.ResponseEntity;
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
}
