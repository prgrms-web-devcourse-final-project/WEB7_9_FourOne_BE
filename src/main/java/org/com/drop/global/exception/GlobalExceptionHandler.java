package org.com.drop.global.exception;

import org.com.drop.global.rsData.RsData;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

	@ExceptionHandler(ServiceException.class)
	public ResponseEntity<ErrorResponse> handleServiceException(ServiceException exception) {
		ErrorCode errorCode = exception.getErrorCode();
		return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.errorResponse(errorCode));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<RsData<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
		BindingResult bindingResult = exception.getBindingResult();
		FieldError firstError = exception.getBindingResult().getFieldError();

		ErrorCode errorCode = ErrorCode.INVALID_PARAMETER;

		if (firstError != null) {
			String field = firstError.getField();

			switch (field) {
				case "name" -> errorCode = ErrorCode.PRODUCT_INVALID_PRODUCT;
				default -> errorCode = ErrorCode.INVALID_PARAMETER;
			}
		}

		return ResponseEntity
			.status(errorCode.getStatus())
			.body(
				new RsData<>(
					errorCode.name(),
					errorCode.getCode(),
					errorCode.getMessage(),
					null
				));
	}
}
