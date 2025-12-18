import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.checkstyle
import org.gradle.kotlin.dsl.jacoco

plugins {
	java
	id("org.springframework.boot") version "3.5.8"
	id("io.spring.dependency-management") version "1.1.6"
	checkstyle
	jacoco
}

group = "org.com"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	/** Spring Boot */
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-mail")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")

	/** DB */
	runtimeOnly("com.mysql:mysql-connector-j")
	runtimeOnly("com.h2database:h2")

	/** Lombok */
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	/** JWT */
	implementation("io.jsonwebtoken:jjwt-api:0.11.5")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

	/** Swagger */
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.0")

	/** Test */
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testImplementation("org.springframework.security:spring-security-test")

	/** Testcontainers */
	testImplementation(platform("org.testcontainers:testcontainers-bom:1.20.4"))
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:mysql")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

