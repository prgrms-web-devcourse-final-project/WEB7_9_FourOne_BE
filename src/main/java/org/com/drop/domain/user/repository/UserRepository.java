package org.com.drop.domain.user.repository;

import org.com.drop.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
	boolean existsByEmail(String email);
	boolean existsByNickname(String nickname);
}
