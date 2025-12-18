plugins {
	java
	id("org.springframework.boot") version "3.5.8"
	id("io.spring.dependency-management") version "1.1.6"
	checkstyle
	jacoco
}

group = "org.com"
version = "0.0.1-SNAPSHOT"
description = "drop"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

/* ===========================
 * Dependencies
 * =========================== */
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
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	/** Testcontainers */
	testImplementation(platform("org.testcontainers:testcontainers-bom:1.20.4"))
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:mysql")
}

/* ===========================
 * Test 공통 설정
 * =========================== */
tasks.withType<Test>().configureEach {
	useJUnitPlatform()

	testLogging {
		events("PASSED", "FAILED", "SKIPPED")
		showExceptions = true
		showCauses = true
		showStackTraces = true
	}

	// Jacoco exec 파일 분리 저장
	extensions.configure(org.gradle.testing.jacoco.plugins.JacocoTaskExtension::class) {
		val execName = if (name == "test") "test.exec" else "$name.exec"
		destinationFile = layout.buildDirectory.file("jacoco/$execName").get().asFile
	}

	// 실패 테스트 요약 수집
	val failed = mutableListOf<String>()
	addTestListener(object : org.gradle.api.tasks.testing.TestListener {
		override fun beforeSuite(suite: org.gradle.api.tasks.testing.TestDescriptor) {}
		override fun beforeTest(testDescriptor: org.gradle.api.tasks.testing.TestDescriptor) {}
		override fun afterTest(desc: org.gradle.api.tasks.testing.TestDescriptor, result: org.gradle.api.tasks.testing.TestResult) {
			if (result.resultType == org.gradle.api.tasks.testing.TestResult.ResultType.FAILURE) {
				failed += "${desc.className}#${desc.name}"
			}
		}

		override fun afterSuite(suite: org.gradle.api.tasks.testing.TestDescriptor, result: org.gradle.api.tasks.testing.TestResult) {
			if (suite.parent == null && failed.isNotEmpty()) {
				println("\n❌ FAILED TESTS (${failed.size})")
				failed.forEachIndexed { i, f -> println("${i + 1}. $f") }

				val out = layout.buildDirectory.file("reports/tests/failed-tests.txt").get().asFile
				out.parentFile.mkdirs()
				out.writeText(failed.joinToString("\n"))
			}
		}
	})
}

/* ===========================
 * Checkstyle
 * =========================== */
checkstyle {
	toolVersion = "8.42"
	configFile = file("${rootDir}/naver-checkstyle-rules.xml")
	configProperties["suppressionFile"] =
		"${rootDir}/naver-checkstyle-suppressions.xml"
	maxWarnings = 0
}

/* ===========================
 * JaCoCo
 * =========================== */
jacoco {
	toolVersion = "0.8.12"
}

val coverageExcludes = listOf(
	"**/*Application*",
	"**/config/**",
	"**/dto/**",
	"**/exception/**",
	"**/vo/**",
	"**/global/**",
	"**/Q*.*",
	"**/*\$*Companion*.*"
)

/* ===========================
 * unit test → jacoco
 * =========================== */
tasks.test {
	finalizedBy(tasks.jacocoTestReport)
}

/* ===========================
 * unit test report
 * =========================== */
tasks.jacocoTestReport {
	dependsOn(tasks.test)

	executionData(layout.buildDirectory.file("jacoco/test.exec"))

	reports {
		xml.required.set(true)
		html.required.set(true)
	}

	classDirectories.setFrom(
		files(
			classDirectories.files.map {
				fileTree(it) { exclude(coverageExcludes) }
			}
		)
	)
}

/* ===========================
 * fullTest (unit + integration)
 * =========================== */
tasks.register<Test>("fullTest") {
	group = "verification"
	description = "Run unit + integration tests"

	testClassesDirs = sourceSets["test"].output.classesDirs
	classpath = sourceSets["test"].runtimeClasspath

	useJUnitPlatform()
	shouldRunAfter(tasks.test)

	extensions.configure(org.gradle.testing.jacoco.plugins.JacocoTaskExtension::class) {
		destinationFile = layout.buildDirectory.file("jacoco/fullTest.exec").get().asFile
	}
}

/* ===========================
 * full jacoco report
 * =========================== */
tasks.register<JacocoReport>("jacocoFullTestReport") {
	dependsOn(tasks.named("fullTest"))

	executionData(
		layout.buildDirectory.file("jacoco/test.exec"),
		layout.buildDirectory.file("jacoco/fullTest.exec")
	)

	reports {
		xml.required.set(true)
		html.required.set(true)
		csv.required.set(true)
	}

	val main = sourceSets["main"]
	sourceDirectories.setFrom(main.allSource.srcDirs)
	classDirectories.setFrom(
		files(
			main.output.classesDirs.map {
				fileTree(it) { exclude(coverageExcludes) }
			}
		)
	)
}



