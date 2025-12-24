package org.com.drop.global.aws;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PreSignedUrlRequest(
	@NotNull(message = "INVALID_IMAGE_TYPE")
	@Pattern(
		regexp = "^image/.*",
		message = "INVALID_IMAGE_TYPE"
	)
	String contentType,
	@NotNull(message = "INVALID_IMAGE_SIZE")
	@Max(
		value = 10 * 1024 * 1024,
		message = "INVALID_IMAGE_SIZE"
	)
	Long contentLength
) { }
