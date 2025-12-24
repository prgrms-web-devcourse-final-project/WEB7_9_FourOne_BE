package org.com.drop.global.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class CursorPaginationUtil {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
	private static final String DELIMITER = "|";

	public static String encodeCursor(LocalDateTime timestamp, Long id) {
		if (timestamp == null || id == null) {
			return null;
		}
		String cursorString = timestamp.format(FORMATTER) + DELIMITER + id;
		return Base64.getEncoder().encodeToString(cursorString.getBytes());
	}

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
			LocalDateTime timestamp = LocalDateTime.parse(parts[0], FORMATTER);
			Long id = Long.parseLong(parts[1]);
			return new Cursor(timestamp, id);
		} catch (Exception e) {
			return null;
		}
	}

	public record Cursor(LocalDateTime timestamp, Long id) {}
}
