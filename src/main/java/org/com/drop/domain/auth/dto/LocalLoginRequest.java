package org.com.drop.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LocalLoginRequest(
	@NotBlank
	@Size(min = 3, max = 100)
	@Email
	String email,

	@NotBlank
	@Size(min = 8, max = 20)
	String password
) { }
