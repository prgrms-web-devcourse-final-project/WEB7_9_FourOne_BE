package org.com.drop.global.config;

import java.util.List;

import org.com.drop.global.security.auth.LoginUser;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

	static {
		SpringDocUtils.getConfig().addAnnotationsToIgnore(LoginUser.class);
	}
	@Bean
	public OpenAPI customOpenApi() {
		Server prodServer = new Server();
		prodServer.setUrl("https://api.p-14626.khee.store");
		prodServer.setDescription("운영 서버");

		Server localServer = new Server();
		localServer.setUrl("http://localhost:8080");
		localServer.setDescription("로컬 테스트");

		String jwtSchemeName = "BearerAuth";
		SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);
		Components components = new Components()
			.addSecuritySchemes(jwtSchemeName, new SecurityScheme()
				.name(jwtSchemeName)
				.type(SecurityScheme.Type.HTTP)
				.scheme("bearer")
				.bearerFormat("JWT"));

		return new OpenAPI()
			.info(new Info()
				.title("Drop Project API")
				.description("상품 등록 및 인증 API 문서")
				.version("v1.0.0"))
			.servers(List.of(prodServer, localServer))
			.addSecurityItem(securityRequirement)
			.components(components);
	}
}
