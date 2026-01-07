package org.com.drop.domain.notification.service;

import org.com.drop.domain.notification.dto.NotificationResponse;
import org.com.drop.domain.notification.entity.Notification;
import org.com.drop.domain.notification.repository.NotificationEmitterRepository;
import org.com.drop.domain.notification.repository.NotificationRepository;
import org.com.drop.domain.user.entity.User;
import org.com.drop.global.exception.ErrorCode;
import org.com.drop.global.exception.ServiceException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.transaction.Transactional;
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

	@Transactional
	public void addNotification(User actor, String msg) {
		Notification notification = new Notification(actor, msg);
		notificationRepository.save(notification);
		NotificationResponse response = new NotificationResponse(notification);
		sendTo(actor.getId(), response);
	}

	public Notification findById( User  actor, Long notificationId) {
		Notification notification = notificationRepository.findById(notificationId)
			.orElseThrow(()-> new ServiceException(ErrorCode.NOTIFICATION_NOT_FOUND, "알림 없습니다."));
		if (!notification.getUser().getId().equals(actor.getId())) {
			throw new ServiceException(ErrorCode.AUTH_ACCESS_DENIED, "권한이 없습니다.");
		}
		return notification;
	}

	public Page<Notification> findByUser(User actor, Pageable pageable) {
		return notificationRepository.findAllByUserId(actor.getId(), pageable);
	}

	@Transactional
	public void deleteNotificationById(User  actor, Long notificationId) {
		Notification notification = findById(actor, notificationId);
		notificationRepository.delete(notification);
	}

	@Transactional
	public Notification read(User actor, Long notificationId) {
		Notification notification = findById(actor, notificationId);
		notification.markAsRead();
		return notification;
	}
}
