package org.com.drop.global.security.auth;

import org.com.drop.domain.user.entity.User;
import org.com.drop.domain.user.service.UserService;
import org.com.drop.global.exception.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

	private final UserService userService;

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.getParameterAnnotation(LoginUser.class) != null
			&& User.class.equals(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(
		MethodParameter parameter,
		ModelAndViewContainer mavContainer,
		NativeWebRequest webRequest,
		WebDataBinderFactory binderFactory
	) throws Exception {

		LoginUser loginUser = parameter.getParameterAnnotation(LoginUser.class);
		boolean required = loginUser != null ? loginUser.required() : true;

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		// 1. 인증 객체가 없는 경우
		if (authentication == null) {
			if (!required) {
				return null;
			}
			throw ErrorCode.USER_UNAUTHORIZED.serviceException("인증 정보가 없습니다.");
		}

		Object principal = authentication.getPrincipal();

		// 2. 익명 사용자(anonymousUser)인 경우
		if ("anonymousUser".equals(principal)) {

			if (!required) {
				return null;
			}
			throw ErrorCode.USER_UNAUTHORIZED.serviceException("인증 정보가 없습니다.");
		}

		// 3. principal이 UserDetails가 아닌 경우
		if (!(principal instanceof UserDetails)) {
			if (!required) {
				return null;
			}
			throw ErrorCode.USER_UNAUTHORIZED.serviceException("인증 정보가 유효하지 않습니다.");
		}

		UserDetails userDetails = (UserDetails) authentication.getPrincipal();
		String email = userDetails.getUsername();

		return userService.findUserByEmail(email);
	}
}
