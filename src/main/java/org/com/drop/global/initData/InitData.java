package org.com.drop.global.initData;

import java.time.LocalDateTime;

import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class InitData {

	private final UserRepository userRepository;
	@Autowired
	@Lazy
	private InitData self;

	@Bean
	ApplicationRunner baseInitData() {
		return args -> {
			self.work();
		};
	}

	@Transactional
	public void work() {

		User user1 = User.builder()
			.email("user1@example.com")
			.nickname("유저1")
			.password("12345678")
			.loginType(User.LoginType.LOCAL)
			.role(User.UserRole.USER)
			.createdAt(LocalDateTime.now())
			.penaltyCount(0)
			.build();
		userRepository.save(user1);
	}

}
