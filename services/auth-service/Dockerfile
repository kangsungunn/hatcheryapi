# Auth Service Dockerfile
# 루트 디렉토리에서 빌드: docker build -f services/auth-service/Dockerfile -t auth-service .

# 1단계: 빌드
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Gradle 래퍼 및 설정 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 서브프로젝트 복사
COPY gateway gateway
COPY services services

# 실행 권한 부여 및 빌드
RUN chmod +x gradlew && ./gradlew :services:auth-service:build -x test

# 2단계: 실행
FROM eclipse-temurin:21-jre
WORKDIR /app

# curl 설치 (헬스체크용)
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 빌드된 JAR 파일 복사
COPY --from=builder /app/services/auth-service/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8081

# 헬스체크
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]