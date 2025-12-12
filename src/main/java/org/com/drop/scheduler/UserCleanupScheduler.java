package org.com.drop.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserCleanupScheduler {

	private final UserRepository userRepository;

	@Scheduled(cron = "0 0 0 * * ?")
	public void deleteSoftDeletedUsers() {
		LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
		List<User> usersToDelete = userRepository.findAllByDeletedAtBefore(cutoffDate);
		userRepository.deleteAll(usersToDelete);
	}
}
