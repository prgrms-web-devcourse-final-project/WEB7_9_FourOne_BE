package org.com.drop.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
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

    @Column
    private String nickname;

    @Column(nullable = false)
    private String password;   // 암호화 저장

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginType loginType;

    @Column
    private String userProfile;  // 프로필 이미지 URL

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column
    private String kakaoId;  // 소셜로그인 전용

    @Column
    private LocalDateTime deletedAt; // soft-delete 용

    // --- Enum 정의 ---
    public enum LoginType {
        LOCAL, KAKAO
    }

    public enum UserRole {
        USER, ADMIN
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.role == null) this.role = UserRole.USER;
    }
}
