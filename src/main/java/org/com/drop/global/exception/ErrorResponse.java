package org.com.drop.global.exception;

import lombok.Builder;
@Builder
public record ErrorResponse(
	String code,
	int httpStatus,
	String message
) {
	public ErrorResponse(ErrorCode errorCode) {
		this(
			errorCode.name(),
			errorCode.getStatus().value(),
			errorCode.getMessage()
		);
	}
}
