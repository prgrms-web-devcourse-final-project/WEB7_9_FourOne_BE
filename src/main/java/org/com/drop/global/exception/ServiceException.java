package org.com.drop.global.exception;

import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {

	private final ErrorCode errorCode;

	public ServiceException(ErrorCode errorCode, String message, Object... args) {
		super(String.format(message, args));
		this.errorCode = errorCode;
	}
}
