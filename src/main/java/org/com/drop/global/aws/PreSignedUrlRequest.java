package org.com.drop.global.aws;

import jakarta.validation.constraints.NotNull;

public record PreSignedUrlRequest(
	@NotNull
	String contentType,
	@NotNull
	Long contentLength
) { }
