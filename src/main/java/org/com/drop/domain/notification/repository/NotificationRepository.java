package org.com.drop.domain.notification.repository;

import java.util.List;

import org.com.drop.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
	List<Notification> findAllByUserId(Long id);
}
