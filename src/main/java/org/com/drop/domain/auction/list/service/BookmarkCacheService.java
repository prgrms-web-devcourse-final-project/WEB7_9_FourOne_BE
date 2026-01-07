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

			Set<Long> result = productIds.stream()
				.filter(id -> !EMPTY_MARKER.equals(id))
				.map(Long::valueOf)
				.collect(Collectors.toSet());

			return result.isEmpty() ? null : result;
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

			// 기존 데이터 삭제 후 새로 저장
			redisTemplate.delete(key);

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

			invalidateUserBookmarkCache(userId);
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

			redisTemplate.opsForSet().remove(key, EMPTY_MARKER);

			redisTemplate.opsForSet().add(key, productId.toString());

			redisTemplate.expire(key, CACHE_TTL_HOURS, TimeUnit.HOURS);
		} catch (DataAccessException e) {
			log.warn("Redis 찜 추가 실패 - userId: {}, productId: {}", userId, productId, e);
			invalidateUserBookmarkCache(userId);
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

			Long removedCount = redisTemplate.opsForSet().remove(key, productId.toString());

			if (removedCount != null && removedCount > 0) {
				log.debug("Redis 북마크 삭제 성공 - userId: {}, productId: {}, removedCount: {}",
					userId, productId, removedCount);

				Long size = redisTemplate.opsForSet().size(key);

				if (size != null && size == 0) {
					redisTemplate.opsForSet().add(key, EMPTY_MARKER);
				}

				redisTemplate.expire(key, CACHE_TTL_HOURS, TimeUnit.HOURS);
			} else {
				log.debug("삭제할 북마크가 Redis에 없음 - userId: {}, productId: {}", userId, productId);
			}
		} catch (DataAccessException e) {
			log.error("Redis 북마크 삭제 실패 - userId: {}, productId: {}", userId, productId, e);
			invalidateUserBookmarkCache(userId);
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

	/**
	 * 캐시가 존재하는지 확인
	 */
	public boolean hasCache(Long userId) {
		try {
			String key = USER_BOOKMARK_KEY_PREFIX + userId;
			return Boolean.TRUE.equals(redisTemplate.hasKey(key));
		} catch (DataAccessException e) {
			log.warn("Redis 캐시 확인 실패 - userId: {}", userId, e);
			return false;
		}
	}
}
