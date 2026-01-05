package org.com.drop.global.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * 커서 기반 페이징 유틸리티
 */
public class CursorPaginationUtil {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
	private static final String DELIMITER = "|";

	/**
	 * 일반 커서 인코딩 (NEWEST, CLOSING)
	 */
	public static String encodeCursor(LocalDateTime timestamp, Long id) {
		if (timestamp == null || id == null) {
			return null;
		}
		String cursorString = timestamp.format(FORMATTER) + DELIMITER + id;
		return Base64.getEncoder().encodeToString(cursorString.getBytes());
	}

	/**
	 * 인기순 커서 인코딩 (POPULAR)
	 */
	public static String encodePopularCursor(Integer score, Long id) {
		if (score == null || id == null) {
			return null;
		}
		String cursorString = score + DELIMITER + id;
		return Base64.getEncoder().encodeToString(cursorString.getBytes());
	}

	/**
	 * 커서 디코딩
	 */
	public static Cursor decodeCursor(String encodedCursor) {
		if (encodedCursor == null || encodedCursor.isBlank()) {
			return null;
		}
		try {
			String decoded = new String(Base64.getDecoder().decode(encodedCursor));
			String[] parts = decoded.split("\\" + DELIMITER);
			if (parts.length != 2) {
				return null;
			}

			// 첫 번째 파트가 숫자(인기순)인지 확인
			if (parts[0].matches("\\d+")) {
				return new Cursor(null, Long.parseLong(parts[1]), Integer.parseInt(parts[0]));
			} else {
				// 날짜 형식이면 (NEWEST, CLOSING)
				LocalDateTime timestamp = LocalDateTime.parse(parts[0], FORMATTER);
				return new Cursor(timestamp, Long.parseLong(parts[1]), null);
			}
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 커서 데이터 클래스
	 */
	public record Cursor(LocalDateTime timestamp, Long id, Integer score) {

		public boolean isPopularCursor() {
			return score != null;
		}

		public boolean isTimestampCursor() {
			return timestamp != null;
		}
	}
}
