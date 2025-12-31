# API Gateway 실행 가이드

## 🚀 localhost:8080으로 실행하는 방법

### 방법 1: Gradle로 직접 실행 (권장)

```bash
# Windows
cd api.kroaddy.site
.\gradlew.bat :gateway:bootRun

# Linux/Mac
cd api.kroaddy.site
./gradlew :gateway:bootRun
```

### 방법 2: Docker로 실행 (가장 간단) ⭐

```bash
cd api.kroaddy.site

# 루트 Dockerfile 사용 (Gateway 전용)
docker build -t gateway .
docker run -p 8080:8080 gateway
```

**또는 실행 스크립트 사용:**

**Windows:**
```bash
cd api.kroaddy.site
docker-run.bat
```

**Linux/Mac:**
```bash
cd api.kroaddy.site
chmod +x docker-run.sh
./docker-run.sh
```

### 방법 3: Docker Compose로 실행 (Gateway만)

```bash
cd api.kroaddy.site

# Gateway만 실행 (다른 서비스는 제외)
docker-compose up gateway
```

### 방법 4: JAR 파일로 실행

```bash
cd api.kroaddy.site

# 빌드
./gradlew :gateway:build

# 실행
java -jar gateway/build/libs/gateway-0.0.1-SNAPSHOT.jar
```

## 📝 실행 확인

실행 후 다음 URL로 접속하여 확인:

- **Gateway**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/docs
- **API Docs**: http://localhost:8080/v3/api-docs

## ⚠️ 주의사항

1. **다른 서비스 의존성**: Gateway는 다른 서비스(oauth-service, user-service 등)에 라우팅하지만, Gateway만 실행해도 기본 기능은 동작합니다.

2. **환경 변수**: 필요시 환경 변수를 설정하세요:
   ```bash
   export KAKAO_REST_API_KEY=your_key
   export JWT_SECRET=your_secret
   ```

3. **포트 충돌**: 8080 포트가 이미 사용 중이면 다른 포트로 변경하거나 기존 프로세스를 종료하세요.

## 🔧 문제 해결

### 포트가 이미 사용 중인 경우
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/Mac
lsof -ti:8080 | xargs kill -9
```

### 빌드 오류가 발생하는 경우
```bash
# 클린 빌드
./gradlew clean :gateway:build

# 의존성 새로고침
./gradlew :gateway:dependencies --refresh-dependencies
```

