
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
	java
	id("org.springframework.boot") version "3.3.4"
	id("io.spring.dependency-management") version "1.1.6"
	id("checkstyle")
}

group = "org.com"
version = "0.0.1-SNAPSHOT"
description = "drop"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {

	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")

	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	developmentOnly("org.springframework.boot:spring-boot-devtools")

	runtimeOnly("com.h2database:h2")
	runtimeOnly("com.mysql:mysql-connector-j:8.4.0")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")

	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:mysql")

	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

checkstyle {
	toolVersion = "8.42"
	configFile = file("${rootDir}/naver-checkstyle-rules.xml")
	configProperties["suppressionFile"] = "${rootDir}/naver-checkstyle-suppressions.xml"
	maxWarnings = 0
}
