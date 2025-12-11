package org.com.drop.domain.user.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.com.drop.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
	boolean existsByEmail(String email);
	boolean existsByNickname(String nickname);
	Optional<User> findByEmail(String email);
	List<User> findAllByDeletedAtBefore(LocalDateTime dateTime);
}
