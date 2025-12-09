package org.com.drop.domain.user.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users", indexes = {@Index(name = "idx_user_email", columnList = "email")}, uniqueConstraints = {
	@UniqueConstraint(name = "uq_user_email", columnNames = "email"),
	@UniqueConstraint(name = "uq_user_nickname", columnNames = "nickname")})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String email;

	@Column(unique = true)
	private String nickname;

	@Column(nullable = false)
	private String password;   // 암호화 저장

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private LoginType loginType;

	private String userProfile;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserRole role;

	private String kakaoId;  // 소셜로그인 전용

	@Column
	private LocalDateTime deletedAt; // soft-delete 용

	public enum LoginType { LOCAL, KAKAO }

	public enum UserRole { USER, ADMIN }
}
