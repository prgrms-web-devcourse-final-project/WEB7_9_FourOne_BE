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

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")

	// Spring
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")

	// DB
	runtimeOnly("com.h2database:h2")
	runtimeOnly("com.mysql:mysql-connector-j")

	// Lombok
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	developmentOnly("org.springframework.boot:spring-boot-devtools")

	// Test
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	// JWT
	implementation("io.jsonwebtoken:jjwt-api:0.11.5")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

	// Swagger
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.4")

	// Email
	implementation ("org.springframework.boot:spring-boot-starter-mail")

	// Redis
	implementation ("org.springframework.boot:spring-boot-starter-data-redis")
	implementation("org.redisson:redisson-spring-boot-starter:3.24.3")

	implementation("io.github.resilience4j:resilience4j-spring-boot3:2.1.0")

	testImplementation("org.testcontainers:testcontainers:1.20.1")
	testImplementation("org.testcontainers:mysql:1.20.1")
	testImplementation("org.testcontainers:junit-jupiter:1.20.1")

    //aws
    implementation("io.awspring.cloud:spring-cloud-aws-starter-s3:3.1.1")
    implementation("org.apache.tika:tika-core:2.9.1")
}

/** ===========================
 *  Checkstyle
 *  =========================== */
checkstyle {
	toolVersion = "8.42"
	configFile = file("${rootDir}/naver-checkstyle-rules.xml")
	configProperties["suppressionFile"] = "${rootDir}/naver-checkstyle-suppressions.xml"
	maxWarnings = 0
}

/** ===========================
 *  JaCoCo 공통 설정
 *  =========================== */
jacoco {
	toolVersion = "0.8.12"
}

/** 커버리지 제외 */
val coverageExcludes = listOf(
	// application / config
	"**/*Application*",
	"**/*Config*",

	// init / runner
	"**/initdata/**",
	"**/*InitData*",
	"**/*Runner*",

	// infra / external
	"**/infra/**",
	"**/global/**",
	"**/global/aws/**",

	// scheduler / batch
	"**/*Scheduler*",

	// dto / request / response / vo
	"**/*Request*",
	"**/*Response*",
	"**/*Dto*",
	"**/dto/**",
	"**/vo/**",

	// exception
	"**/exception/**",

	// querydsl / kotlin companion
	"**/Q*.*",
	"**/*\$*Companion*.*"
)

/** ===========================
 *  Test 공통 설정 (로깅/요약/실패수집)
 *  =========================== */
tasks.withType<Test>().configureEach {
	useJUnitPlatform()

	// 테스트 실행 로그
	testLogging {
		events("PASSED", "FAILED", "SKIPPED")
		showStandardStreams = true
		showExceptions = true
		showCauses = true
		showStackTraces = true
	}

	// jacoco exec 파일 저장 위치 명시
	extensions.configure(org.gradle.testing.jacoco.plugins.JacocoTaskExtension::class) {
		val execName = if (name == "test") "test.exec" else "${name}.exec"
		setDestinationFile(layout.buildDirectory.file("jacoco/$execName").get().asFile)
	}

	// 테스트 실패 요약 보고 기능 추가
	val failed = mutableListOf<Triple<String, String, String?>>() // class, method, msg
	addTestListener(object : org.gradle.api.tasks.testing.TestListener {
		override fun beforeSuite(suite: org.gradle.api.tasks.testing.TestDescriptor) {}
		override fun beforeTest(testDescriptor: org.gradle.api.tasks.testing.TestDescriptor) {}
		override fun afterTest(desc: org.gradle.api.tasks.testing.TestDescriptor, result: org.gradle.api.tasks.testing.TestResult) {
			if (result.resultType == org.gradle.api.tasks.testing.TestResult.ResultType.FAILURE) {
				val clazz = desc.className ?: "(unknown-class)"
				val method = desc.name
				val msg = result.exception?.message?.lineSequence()?.firstOrNull()
				failed += Triple(clazz, method, msg)
			}
		}

		override fun afterSuite(suite: org.gradle.api.tasks.testing.TestDescriptor, result: org.gradle.api.tasks.testing.TestResult) {
			if (suite.parent == null) {
				println(
					"""
                    ------------------------
                    ✅ TEST RESULT SUMMARY
                    Total tests : ${result.testCount}
                    Passed : ${result.successfulTestCount}
                    Failed : ${result.failedTestCount}
                    Skipped : ${result.skippedTestCount}
                    ------------------------
                    """.trimIndent()
				)
				val out = layout.buildDirectory.file("reports/tests/failed-tests.txt").get().asFile
				out.parentFile.mkdirs()
				if (failed.isNotEmpty()) {
					val RED = "\u001B[31m"
					val RESET = "\u001B[0m"
					println("❌ FAILED TESTS (${failed.size})")
					failed.forEachIndexed { i, (c, m, msg) ->
						println("${RED}${i + 1}. $c#$m${if (msg != null) " — $msg" else ""}${RESET}")
					}
					out.printWriter().use { pw ->
						pw.println("FAILED TESTS (${failed.size})")
						failed.forEach { (c, m, msg) ->
							pw.println("$c#$m${if (msg != null) " — $msg" else ""}")
						}
						pw.println()
						pw.println("Patterns for --tests:")
						failed.forEach { (c, m, _) -> pw.println("--tests \"$c.$m\"") }
					}
					println("📄 Saved failed list -> ${out.absolutePath}")
				} else {
					out.writeText("No failures 🎉")
				}
			}
		}
	})
}

/** ===========================
 *  기본 test 태스크 (단위 테스트)
 *  =========================== */
tasks.named<Test>("test") {
	// 테스트 태그 필터링 기능 추가
	if (project.findProperty("includeIntegration") == "true") {
		systemProperty("junit.platform.tags.includes", "integration,unit")
	} else {
		systemProperty("junit.platform.tags.excludes", "integration")
	}
	finalizedBy(tasks.jacocoTestReport)
}

/** ===========================
 *  fullTest 태스크 (통합 테스트 포함)
 *  =========================== */
tasks.register<Test>("fullTest") {
	description = "Run unit + integration tests"
	group = "verification"

	val testSourceSet = sourceSets.named("test").get()
	testClassesDirs = testSourceSet.output.classesDirs
	classpath = testSourceSet.runtimeClasspath

	useJUnitPlatform()
	shouldRunAfter(tasks.named("test"))

	extensions.configure(org.gradle.testing.jacoco.plugins.JacocoTaskExtension::class) {
		setDestinationFile(layout.buildDirectory.file("jacoco/fullTest.exec").get().asFile)
	}
	finalizedBy(tasks.named("jacocoFullTestReport"))
}

/** ===========================
 *  jacocoTestReport (단위 테스트 리포트)
 *  =========================== */
tasks.jacocoTestReport {
	dependsOn(tasks.named("test"))

	executionData(layout.buildDirectory.file("jacoco/test.exec"))

	reports {
		xml.required.set(true)
		html.required.set(true)
		csv.required.set(false)

		xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/xml/jacocoTestReport.xml"))
		html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/html"))
	}

	classDirectories.setFrom(
		files(
			classDirectories.files.map {
				fileTree(it) { exclude(coverageExcludes) }
			}
		)
	)
}

/** ===========================
 *  jacocoFullTestReport (통합 테스트 리포트)
 *  =========================== */
tasks.register<JacocoReport>("jacocoFullTestReport") {
	dependsOn(tasks.named("fullTest"))

	executionData(fileTree(layout.buildDirectory.dir("jacoco")) { include("*.exec") })

	reports {
		xml.required.set(true)
		html.required.set(true)
		csv.required.set(true)

		xml.outputLocation.set(layout.buildDirectory.file("reports/jacocoFull/xml/jacocoFullTestReport.xml"))
		html.outputLocation.set(layout.buildDirectory.dir("reports/jacocoFull/html"))
	}

	val main = sourceSets.named("main").get()
	sourceDirectories.setFrom(main.allSource.srcDirs)
	classDirectories.setFrom(
		files(
			main.output.classesDirs.files.map {
				fileTree(it) { exclude(coverageExcludes) }
			}
		)
	)
}
