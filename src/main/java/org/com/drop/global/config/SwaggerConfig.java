package org.com.drop.global.config;

import org.com.drop.global.security.auth.LoginUser;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig {

	static {
		SpringDocUtils.getConfig().addAnnotationsToIgnore(LoginUser.class);
	}

	@Bean
	public OpenAPI openApi() {
		return new OpenAPI()
			.components(new Components()
				.addSecuritySchemes("bearer-key",
					new SecurityScheme()
						.type(SecurityScheme.Type.HTTP)
						.scheme("bearer")
						.bearerFormat("JWT")))
			.info(new Info()
				.title("Drop Project API")
				.description("상품 등록 및 인증 API 문서")
				.version("v1.0.0"));
	}
}
