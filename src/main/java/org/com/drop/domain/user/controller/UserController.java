package org.com.drop.domain.user.controller;

import org.com.drop.domain.user.dto.MyBookmarkPageResponse;
import org.com.drop.domain.user.dto.MyPageResponse;
import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.service.UserService;
import org.com.drop.global.rsdata.RsData;
import org.com.drop.global.security.auth.LoginUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping("/me")
	public RsData<MyPageResponse> me(
		@LoginUser User user) {

		MyPageResponse response = userService.getMe(user);
		return new RsData<>(response);
	}

	@GetMapping("/me/bookmarks")
	public RsData<MyBookmarkPageResponse> getMyBookmarks(
		@LoginUser User user,
		@RequestParam(defaultValue = "1") int page) {

		MyBookmarkPageResponse response = userService.getMyBookmarks(user, page);
		return new RsData<>(response);
	}
}
