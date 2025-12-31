# Gateway Dockerfile - localhost:8080으로 실행
# 사용법:
#   docker build -t gateway .
#   docker run -p 8080:8080 gateway

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

# 실행 권한 부여 및 Gateway 빌드
RUN chmod +x gradlew && ./gradlew :gateway:build -x test

# 2단계: 실행
FROM eclipse-temurin:21-jre
WORKDIR /app

# curl 설치 (헬스체크용)
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 빌드된 Gateway JAR 파일 복사
COPY --from=builder /app/gateway/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 헬스체크 (Gateway의 Swagger UI 또는 루트 경로 확인)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/docs || curl -f http://localhost:8080/ || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]

