package org.com.drop.domain.notification.controller;

import java.util.List;

import org.com.drop.domain.notification.dto.NotificationResponse;
import org.com.drop.domain.notification.entity.Notification;
import org.com.drop.domain.notification.service.NotificationService;
import org.com.drop.domain.user.entity.User;
import org.com.drop.global.rsdata.RsData;
import org.com.drop.global.security.auth.LoginUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

	private final NotificationService notificationService;

	@GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter subscribe(@LoginUser User actor) {
		return notificationService.subscribe(actor.getId());
	}

	@GetMapping
	public RsData<List<NotificationResponse>> getNotification(
		@LoginUser User actor,
		@PageableDefault(size = 20, sort = "sendAt", direction = Sort.Direction.DESC)
		Pageable pageable
	) {
		List<NotificationResponse> notificationList =
			notificationService.findByUser(actor, pageable).stream().map(NotificationResponse::new).toList();
		return new RsData<>(
			notificationList
		);
	}

	@GetMapping("/{notificationId}")
	public RsData<NotificationResponse> getNotificationById(
		@LoginUser User actor,
		@PathVariable Long notificationId
	) {
		Notification notification = notificationService.findById(actor, notificationId);
		return new RsData<>(
			new NotificationResponse(notification)
		);
	}

	@PutMapping("/{notificationId}")
	public RsData<NotificationResponse> readNotification(
		@LoginUser User actor,
		@PathVariable Long notificationId
	) {
		Notification notification = notificationService.read(actor, notificationId);
		return new RsData<>(
			new NotificationResponse(notification)
		);
	}

	@DeleteMapping("/{notificationId}")
	public RsData<Void> deleteNotification(
		@LoginUser User actor,
		@PathVariable Long notificationId
	) {
		notificationService.deleteNotificationById(actor, notificationId);
		return new RsData<>(
			null
		);
	}

}
