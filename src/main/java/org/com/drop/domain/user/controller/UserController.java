package org.com.drop.domain.user.controller;

import org.com.drop.domain.user.dto.GetCurrentUserInfoResponse;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.service.UserService;
import org.com.drop.global.rsdata.RsData;
import org.com.drop.global.security.auth.LoginUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping("/me")
	public RsData<GetCurrentUserInfoResponse> me(
		@LoginUser User user) {

		GetCurrentUserInfoResponse response = userService.getMe(user);
		return new RsData<>(response);
	}
}
