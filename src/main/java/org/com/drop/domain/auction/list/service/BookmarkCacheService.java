package org.com.drop.domain.auction.list.service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자 중심의 찜 캐싱 서비스 (Inverted Index)
 * Redis Key 패턴: bookmark:user:{userId} -> Set<String> (Product IDs)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookmarkCacheService {

	private final RedisTemplate<String, String> redisTemplate;
	private static final String USER_BOOKMARK_KEY_PREFIX = "bookmark:user:";
	private static final long CACHE_TTL_HOURS = 1;
	private static final String EMPTY_MARKER = "EMPTY";

	/**
	 * 사용자의 찜한 상품 ID 목록 조회 (Cache-Aside Pattern)
	 *
	 * @param userId 사용자 ID
	 * @return 찜한 상품 ID Set, 캐시 미스시 null
	 */
	public Set<Long> getBookmarkedProductIds(Long userId) {
		try {
			String key = USER_BOOKMARK_KEY_PREFIX + userId;
			Set<String> productIds = redisTemplate.opsForSet().members(key);

			if (productIds == null || productIds.isEmpty()) {
				return null;
			}

			return productIds.stream()
				.filter(id -> !EMPTY_MARKER.equals(id))
				.map(Long::valueOf)
				.collect(Collectors.toSet());
		} catch (DataAccessException e) {
			log.warn("Redis 조회 실패, DB로 폴백 - userId: {}", userId, e);
			return null;
		}
	}

	/**
	 * DB에서 조회한 사용자의 찜 목록을 Redis에 캐싱
	 *
	 * @param userId 사용자 ID
	 * @param productIds 찜한 상품 ID 목록
	 */
	public void cacheUserBookmarks(Long userId, List<Long> productIds) {
		try {
			String key = USER_BOOKMARK_KEY_PREFIX + userId;

			if (productIds.isEmpty()) {
				redisTemplate.opsForSet().add(key, EMPTY_MARKER);
			} else {
				String[] ids = productIds.stream()
					.map(String::valueOf)
					.toArray(String[]::new);
				redisTemplate.opsForSet().add(key, ids);
			}

			redisTemplate.expire(key, CACHE_TTL_HOURS, TimeUnit.HOURS);
			log.debug("Redis 유저 찜 목록 캐싱 완료 - userId: {}, count: {}", userId, productIds.size());
		} catch (DataAccessException e) {
			log.error("Redis 캐싱 실패 - userId: {}", userId, e);
		}
	}

	/**
	 * 찜 추가 (Write-Through)
	 *
	 * @param userId 사용자 ID
	 * @param productId 상품 ID
	 */
	public void addBookmark(Long userId, Long productId) {
		try {
			String key = USER_BOOKMARK_KEY_PREFIX + userId;
			if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
				redisTemplate.opsForSet().add(key, productId.toString());
			}
		} catch (DataAccessException e) {
			log.warn("Redis 찜 추가 실패 - userId: {}, productId: {}",
				userId, productId, e);
		}
	}

	/**
	 * 찜 해제 (Write-Through)
	 *
	 * @param userId 사용자 ID
	 * @param productId 상품 ID
	 */
	public void removeBookmark(Long userId, Long productId) {
		try {
			String key = USER_BOOKMARK_KEY_PREFIX + userId;
			if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
				redisTemplate.opsForSet().remove(key, productId.toString());
			}
		} catch (DataAccessException e) {
			log.warn("Redis 찜 해제 실패 - userId: {}, productId: {}",
				userId, productId, e);
		}
	}

	/**
	 * 사용자의 찜 캐시 무효화
	 *
	 * @param userId 사용자 ID
	 */
	public void invalidateUserBookmarkCache(Long userId) {
		try {
			String key = USER_BOOKMARK_KEY_PREFIX + userId;
			redisTemplate.delete(key);
			log.debug("사용자 찜 캐시 무효화 - userId: {}", userId);
		} catch (DataAccessException e) {
			log.warn("Redis 캐시 삭제 실패 - userId: {}", userId, e);
		}
	}
}
