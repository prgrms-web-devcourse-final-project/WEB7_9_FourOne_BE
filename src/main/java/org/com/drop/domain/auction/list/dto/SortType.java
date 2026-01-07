package org.com.drop.domain.auction.list.dto;

import org.com.drop.global.exception.ErrorCode;

public enum SortType {
	NEWEST("newest", "최신순"),
	CLOSING("closing", "마감임박순"),
	POPULAR("popular", "인기순");

	private final String value;
	private final String description;

	SortType(String value, String description) {
		this.value = value;
		this.description = description;
	}

	public static SortType fromValue(String value) {
		if (value == null || value.isBlank()) {
			return NEWEST;
		}

		String normalized = value.trim().toLowerCase();
		for (SortType type : SortType.values()) {
			if (type.value.equals(normalized)) {
				return type;
			}
		}

		// value 대문자
		try {
			return SortType.valueOf(value.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw ErrorCode.AUCTION_INVALID_SORT.serviceException(
				"지원하지 않는 정렬 방식입니다: %s. 가능한 값: newest, closing, popular", value
			);
		}
	}

	public static SortType fromString(String value) {
		try {
			return SortType.valueOf(value.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw ErrorCode.AUCTION_INVALID_SORT.serviceException("지원하지 않는 정렬 방식입니다: %s", value);
		}
	}

	public String getValue() {
		return value;
	}

	public String getDescription() {
		return description;
	}
}
