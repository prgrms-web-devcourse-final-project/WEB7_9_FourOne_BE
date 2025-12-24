package org.com.drop.global.aws;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record PreSignedUrlListRequest(
	@NotEmpty(message = "INVALID_IMAGE") @Valid
	List<PreSignedUrlRequest> requests
) {
}
