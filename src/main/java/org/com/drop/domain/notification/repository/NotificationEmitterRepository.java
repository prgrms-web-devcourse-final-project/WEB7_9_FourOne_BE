package org.com.drop.domain.notification.repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Repository
public class NotificationEmitterRepository {
	private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

	public Map<Long, SseEmitter> getAllEmitters() {
		return emitters;
	}

	public SseEmitter save(Long userId, SseEmitter emitter) {
		emitters.put(userId, emitter);
		return emitter;
	}

	public void delete(Long userId) {
		emitters.remove(userId);
	}

	public SseEmitter get(Long userId) {
		return emitters.get(userId);
	}
}
