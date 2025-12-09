package org.com.drop.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {
	private final ErrorBody error;

	public static ErrorResponse errorResponse(ErrorCode errorCode) {
		return ErrorResponse.builder()
			.error(ErrorBody.builder()
				.code(errorCode.name())
				.status(String.valueOf(errorCode.getStatus().value()))
				.message(errorCode.getMessage())
				.build())
			.build();
	}

	public static ErrorResponse validationError(BindingResult bindingResult) {
		return ErrorResponse.builder()
			.error(ErrorBody.builder()
				.code(ErrorCode.VALIDATION_ERROR.name())
				.status(String.valueOf(HttpStatus.BAD_REQUEST.value()))
				.message("요청 값이 올바르지 않습니다.")
				.build())
			.build();
	}

	@Getter
	@Builder
	public static class ErrorBody {
		private final String code;
		private final String status;
		private final String message;
	}
}
