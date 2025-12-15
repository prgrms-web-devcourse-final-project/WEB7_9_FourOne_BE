package org.com.drop.domain.user.service;

import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.repository.UserRepository;
import org.com.drop.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public User findUserByEmail(String email) {
		return userRepository.findByEmail(email)
			.orElseThrow(() ->
				ErrorCode.USER_NOT_FOUND
					.serviceException("email=%s", email)
			);
	}
}
