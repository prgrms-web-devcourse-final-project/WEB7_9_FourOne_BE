package org.com.drop.domain.notification.entity;

import java.time.LocalDateTime;

import org.com.drop.domain.user.entity.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "notifications",
	indexes = {@Index(name = "idx_notifications_user_id", columnList = "user_id")}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Notification {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false)
	private String message;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime sendAt;

	@Column(nullable = true)
	private LocalDateTime readAt;

	@Builder
	public Notification(User user, String message) {
		this.user = user;
		this.message = message;
	}

	public void markAsRead() {
		if (this.readAt == null) {
			this.readAt = LocalDateTime.now();
		}
	}
}
