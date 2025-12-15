package org.com.drop.domain.auth.email.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailSendRequest(

	@NotBlank
	@Email
	String email

) { }
