package org.com.drop.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserDeleteRequest(
	@NotBlank
	@Size(min = 8, max = 20)
	@Pattern(
		regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()\\-_+]).{8,20}$"
	)
	String password
) { }
