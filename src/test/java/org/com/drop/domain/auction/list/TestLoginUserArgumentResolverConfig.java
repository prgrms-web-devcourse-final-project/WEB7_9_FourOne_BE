package org.com.drop.domain.auction.list;

import java.util.List;

import org.com.drop.domain.user.repository.UserRepository;
import org.com.drop.global.security.auth.LoginUser;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class TestLoginUserArgumentResolverConfig implements WebMvcConfigurer {

	private final UserRepository userRepository;

	public TestLoginUserArgumentResolverConfig(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(new HandlerMethodArgumentResolver() {

			@Override
			public boolean supportsParameter(MethodParameter parameter) {
				return parameter.hasParameterAnnotation(LoginUser.class);
			}

			@Override
			public Object resolveArgument(
				MethodParameter parameter,
				ModelAndViewContainer mavContainer,
				NativeWebRequest webRequest,
				org.springframework.web.bind.support.WebDataBinderFactory binderFactory
			) {
				var auth = SecurityContextHolder.getContext().getAuthentication();
				if (auth == null || !auth.isAuthenticated()) {
					return null;
				}
				return userRepository.findByEmail(auth.getName()).orElse(null);
			}
		});
	}
}
