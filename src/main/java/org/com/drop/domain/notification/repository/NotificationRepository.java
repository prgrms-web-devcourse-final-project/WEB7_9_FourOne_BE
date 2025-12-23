package org.com.drop.domain.notification.repository;

import java.util.List;

import org.com.drop.domain.notification.entity.Notification;
import org.com.drop.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
	List<Notification> findAllByUser(User actor);
}
