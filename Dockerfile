# 첫 번째 스테이지: 빌드 스테이지
FROM gradle:jdk-21-and-23-graal-jammy AS builder

# 작업 디렉토리 설정
WORKDIR /app
COPY .env.properties .env.properties
# Gradle 래퍼 관련 파일 먼저 복사 (gradlew 스크립트와 설정 디렉토리)
COPY gradlew .
COPY gradle gradle

# Gradle 래퍼에 실행 권한 부여
RUN chmod +x gradlew

# 소스 코드와 Gradle 설정 파일 복사
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY naver-checkstyle-rules.xml .

# 종속성 설치
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src src

# 애플리케이션 빌드
RUN ./gradlew build --no-daemon \
    -Dspring.config.import=optional:file:./.env.properties

# 이후 명령어가 편하도록 불필요한 파일 삭제
RUN rm -rf /app/build/libs/*-plain.jar

# 두 번째 스테이지: 실행 스테이지
FROM container-registry.oracle.com/graalvm/jdk:23

# 작업 디렉토리 설정
WORKDIR /app

# 첫 번째 스테이지에서 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 실행할 JAR 파일 지정
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]