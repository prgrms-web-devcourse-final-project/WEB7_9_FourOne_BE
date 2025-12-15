package org.com.drop.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LocalSignUpRequest(
	@NotBlank
	@Size(min = 3, max = 100)
	@Pattern(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
	String email,

	@NotBlank
	@Size(min = 8, max = 20)
	@Pattern(
		regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()\\-_+]).{8,20}$"
	)
	String password,

	@NotBlank
	@Size(min = 3, max = 10)
	@Pattern(regexp = "^[a-zA-Z0-9가-힣]{3,10}$")
	String nickname
) { }
