package org.com.drop.domain.notification.service;

import org.com.drop.domain.notification.repository.NotificationEmitterRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HeartbeatScheduler {

	private final NotificationEmitterRepository notificationEmitterRepository;

	@Scheduled(fixedRate = 15000)
	public void sendHeartbeat() {
		notificationEmitterRepository.getAllEmitters().forEach((userId, emitter) -> {
			try {
				emitter.send(":\n\n");
			} catch (Exception e) {
				notificationEmitterRepository.delete(userId);
			}
		});
	}
}
