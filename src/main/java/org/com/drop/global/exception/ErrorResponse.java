package org.com.drop.global.exception;

import lombok.Builder;
@Builder
public record ErrorResponse(
	String code,
	String status,
	String message
) {
	public static ErrorResponse errorResponse(ErrorCode errorCode) {
		return ErrorResponse.builder()
			.code(errorCode.getCode())
			.status(String.valueOf(errorCode.getStatus().value()))
			.message(errorCode.getMessage())
			.build();
	}
}
