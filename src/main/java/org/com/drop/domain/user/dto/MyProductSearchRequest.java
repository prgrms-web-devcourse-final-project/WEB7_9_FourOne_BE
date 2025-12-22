package org.com.drop.domain.user.dto;

public record MyProductSearchRequest(
	String status // ALL, PENDING, SCHEDULED, LIVE, ENDED, CANCELLED
) {}
