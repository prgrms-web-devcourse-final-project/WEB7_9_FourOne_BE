package org.com.drop.global.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

	@ExceptionHandler(ServiceException.class)
	public ResponseEntity<ErrorResponse> handleServiceException(ServiceException exception) {
		ErrorCode errorCode = exception.getErrorCode();
		log.error(exception.getMessage(), exception);
		return ResponseEntity.status(errorCode.getStatus()).body(new ErrorResponse(errorCode));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(
		MethodArgumentNotValidException exception
	) {
		ErrorCode errorCode;

		try {
			String errorCodeName = exception.getBindingResult().getFieldError().getDefaultMessage();
			errorCode = ErrorCode.valueOf(errorCodeName);
		} catch (Exception e) {
			errorCode = ErrorCode.INVALID_PARAMETER;
		}

		ErrorResponse response = new ErrorResponse(errorCode);

		return ResponseEntity
			.status(response.httpStatus())
			.body(response);
	}
}
