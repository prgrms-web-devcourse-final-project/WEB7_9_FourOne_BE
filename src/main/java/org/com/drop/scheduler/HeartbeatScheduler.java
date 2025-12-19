package org.com.drop.scheduler;

import org.com.drop.domain.notification.repository.NotificationEmitterRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HeartbeatScheduler {

	private final NotificationEmitterRepository notificationEmitterRepository;

	@Scheduled(fixedRate = 15000)
	public void sendHeartbeat() {
		notificationEmitterRepository.getAllEmitters().forEach((userId, emitter) -> {
			try {
				emitter.send(SseEmitter.event().comment("heartbeat"));
			} catch (Exception e) {
				notificationEmitterRepository.delete(userId);
			}
		});
	}
}
