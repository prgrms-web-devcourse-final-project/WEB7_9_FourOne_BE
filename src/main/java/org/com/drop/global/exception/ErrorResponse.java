package org.com.drop.global.exception;

import lombok.Builder;
@Builder
public record ErrorResponse(
	String code,
	int httpStatus,
	String message
) {
	public static ErrorResponse errorResponse(ErrorCode errorCode) {
		return ErrorResponse.builder()
			.code(errorCode.name())
			.httpStatus(errorCode.getStatus().value())
			.message(errorCode.getMessage())
			.build();
	}
}
