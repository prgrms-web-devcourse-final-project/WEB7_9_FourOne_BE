package org.com.drop.domain.notification.service;

import org.com.drop.domain.notification.entity.Notification;
import org.com.drop.domain.notification.repository.NotificationEmitterRepository;
import org.com.drop.domain.notification.repository.NotificationRepository;
import org.com.drop.domain.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

	private static final Long TIMEOUT = 0L;
	private final NotificationEmitterRepository notificationEmitterRepository;
	private final NotificationRepository notificationRepository;

	public SseEmitter subscribe(Long userId) {
		SseEmitter emitter = new SseEmitter(TIMEOUT);

		notificationEmitterRepository.save(userId, emitter);

		emitter.onCompletion(() -> notificationEmitterRepository.delete(userId));
		emitter.onTimeout(() -> notificationEmitterRepository.delete(userId));
		emitter.onError((e) -> notificationEmitterRepository.delete(userId));

		sendTo(userId, "CONNECTED");

		return emitter;
	}

	public void sendTo(Long userId, Object data) {
		SseEmitter emitter = notificationEmitterRepository.get(userId);

		if (emitter != null) {
			try {
				emitter.send(SseEmitter.event()
					.name("notification")
					.data(data)
				);
			} catch (Exception e) {
				notificationEmitterRepository.delete(userId);
			}
		}
	}

	public Notification addNotification(User actor, String msg) {
		Notification notification = new Notification(actor, msg);
		return notificationRepository.save(notification);
	}
}
