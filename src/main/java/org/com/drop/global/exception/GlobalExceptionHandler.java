package org.com.drop.global.exception;

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
	public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException exception) {
		ErrorCode errorCode = getErrorCode(exception.getBindingResult());

		return ResponseEntity
			.status(errorCode.getStatus())
			.body(ErrorResponse.errorResponse(errorCode));
	}

	public ErrorCode getErrorCode(BindingResult bindingResult) {
		FieldError firstError = bindingResult.getFieldError();

		ErrorCode errorCode = ErrorCode.INVALID_PARAMETER;

		if (firstError != null) {
			String field = firstError.getField();

			switch (field) {
				case "name" -> errorCode = ErrorCode.PRODUCT_INVALID_PRODUCT_NAME;
				case "description" -> errorCode = ErrorCode.PRODUCT_INVALID_PRODUCT_DESCRIPTION;
				case "category" -> errorCode = ErrorCode.PRODUCT_INVALID_PRODUCT_CATEGORY;
				case "subCategory" -> errorCode = ErrorCode.PRODUCT_INVALID_PRODUCT_SUB_CATEGORY;
				case "image" -> errorCode = ErrorCode.PRODUCT_INVALID_PRODUCT_IMAGE;
				case "question" -> errorCode = ErrorCode.PRODUCT_INVALID_QUESTION;
				case "answer" -> errorCode = ErrorCode.PRODUCT_INVALID_ANSWER;
			}
		}
		return errorCode;
	}
}
