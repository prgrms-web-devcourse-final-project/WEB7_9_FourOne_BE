package org.com.drop.global.exception;

public class AlreadyProcessedException extends RuntimeException {
	public AlreadyProcessedException() {
		super("Already processed.");
	}

	public AlreadyProcessedException(String message) {
		super(message);
	}
}
