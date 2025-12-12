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
			.code(errorCode.name())
			.status(String.valueOf(errorCode.getCode()))
			.message(errorCode.getMessage())
			.build();
	}
}
