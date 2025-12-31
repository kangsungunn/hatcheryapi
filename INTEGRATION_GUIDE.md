# API 통합 가이드 (Integration Guide)

## 📋 개요

이 문서는 `core.kroaddy.site`의 서비스들을 `api.kroaddy.site`로 통합하는 과정을 안내합니다.

## ✅ 현재 통합 상태

### 통합 완료된 서비스
- ✅ **Gateway** (`api.kroaddy.site/gateway`)
  - 패키지: `com.kroaddy.api`
  - 포트: 8080

- ✅ **User Service** (`api.kroaddy.site/services/user-service`)
  - 패키지: `com.labzang.api` → **변경 필요**: `com.kroaddy.api`
  - 포트: 8082

- ✅ **OAuth Service** (`api.kroaddy.site/services/oauth-service`)
  - 패키지: `com.kroaddy.api` (새로 생성됨)
  - 포트: 8081
  - **주의**: core.kroaddy.site의 소스 코드를 복사하고 패키지를 변경해야 함

### 통합 필요 서비스
- ⚠️ **Auth Service** (`api.kroaddy.site/services/auth-service`)
  - 패키지: `site.protoa.api` → **변경 필요**: `com.kroaddy.api`
  - 포트: 8081
  - **주의**: oauth-service와 기능이 중복될 수 있음. 통합 검토 필요

## 🔧 패키지 구조 통일

### 목표 패키지 구조
모든 서비스는 `com.kroaddy.api` 패키지를 사용해야 합니다.

```
com.kroaddy.api
├── config/          # 설정 클래스
├── auth/            # 인증 관련
├── jwt/             # JWT 토큰 처리
├── google/          # Google OAuth
├── kakao/           # Kakao OAuth
├── naver/           # Naver OAuth
├── log/             # 로깅
└── dto/             # 데이터 전송 객체
```

### 패키지 변경 방법

1. **디렉토리 구조 변경**
   ```bash
   # 예: user-service의 경우
   src/main/java/com/labzang/api → src/main/java/com/kroaddy/api
   ```

2. **Java 파일의 package 선언 변경**
   ```java
   // 변경 전
   package com.labzang.api;
   
   // 변경 후
   package com.kroaddy.api;
   ```

3. **Import 문 변경**
   ```java
   // 변경 전
   import com.labzang.api.jwt.JwtTokenProvider;
   
   // 변경 후
   import com.kroaddy.api.jwt.JwtTokenProvider;
   ```

## 📦 OAuth Service 통합 작업

### 1. 소스 코드 복사
`core.kroaddy.site/oauthservice/src/main/java/com/labzang/api/`의 모든 파일을
`api.kroaddy.site/services/oauth-service/src/main/java/com/kroaddy/api/`로 복사

### 2. 패키지 변경
모든 Java 파일의 패키지를 `com.labzang.api` → `com.kroaddy.api`로 변경

### 3. 필요한 파일 목록
- ✅ `ApiApplication.java` (이미 생성됨)
- ✅ `config/GlobalExceptionHandler.java` (이미 생성됨)
- ✅ `config/RedisConfig.java` (이미 생성됨)
- ✅ `config/WebClientConfig.java` (이미 생성됨)
- ⚠️ `auth/AuthController.java` (패키지 변경 필요)
- ⚠️ `jwt/JwtTokenProvider.java` (패키지 변경 필요)
- ⚠️ `jwt/JwtProperties.java` (패키지 변경 필요)
- ⚠️ `google/` 디렉토리 전체 (패키지 변경 필요)
- ⚠️ `kakao/` 디렉토리 전체 (패키지 변경 필요)
- ⚠️ `naver/` 디렉토리 전체 (패키지 변경 필요)
- ⚠️ `log/` 디렉토리 전체 (패키지 변경 필요)

## 🔍 Import 에러 방지 체크리스트

### 1. 패키지 충돌 확인
- [ ] 모든 서비스가 `com.kroaddy.api` 패키지 사용
- [ ] 동일한 클래스명이 다른 패키지에 존재하지 않는지 확인

### 2. Bean 이름 충돌 확인
- [ ] `@Bean` 메서드 이름이 고유한지 확인
- [ ] `@Component`, `@Service`, `@Repository` 클래스 이름이 고유한지 확인

### 3. 설정 파일 충돌 확인
- [ ] `application.yaml`의 설정 키가 충돌하지 않는지 확인
- [ ] 포트 번호가 중복되지 않는지 확인

### 4. 의존성 충돌 확인
- [ ] 동일한 라이브러리의 버전이 일치하는지 확인
- [ ] `build.gradle`의 의존성이 올바르게 설정되었는지 확인

## 🚨 주의사항

### 1. Auth Service vs OAuth Service
현재 `auth-service`와 `oauth-service`가 모두 존재합니다:
- `auth-service`: `site.protoa.api` 패키지 사용
- `oauth-service`: `com.kroaddy.api` 패키지 사용 (새로 생성)

**권장사항**: 두 서비스의 기능을 비교하여 하나로 통합하거나, 역할을 명확히 분리해야 합니다.

### 2. Bean 이름 충돌
다음 클래스들이 여러 서비스에 존재할 수 있습니다:
- `WebClientConfig` - Bean 이름을 다르게 하거나 하나로 통합
- `RedisConfig` - Bean 이름을 다르게 하거나 하나로 통합
- `JwtTokenProvider` - Bean 이름을 다르게 하거나 하나로 통합

**해결 방법**:
```java
@Bean(name = "oauthWebClient")
public WebClient oauthWebClient() { ... }

@Bean(name = "authWebClient")
public WebClient authWebClient() { ... }
```

### 3. 설정 파일 통합
각 서비스의 `application.yaml`을 확인하여:
- 포트 번호가 중복되지 않는지 확인
- Redis, Database 설정이 올바른지 확인
- OAuth 클라이언트 정보가 올바른지 확인

## 📝 통합 후 검증

### 1. 빌드 테스트
```bash
cd api.kroaddy.site
./gradlew clean build
```

### 2. 컴파일 에러 확인
- [ ] 모든 Java 파일이 컴파일되는지 확인
- [ ] Import 에러가 없는지 확인
- [ ] 패키지 선언이 올바른지 확인

### 3. 런타임 테스트
```bash
# 각 서비스별로 실행 테스트
./gradlew :gateway:bootRun
./gradlew :services:oauth-service:bootRun
./gradlew :services:user-service:bootRun
```

## 🔄 다음 단계

1. **OAuth Service 소스 코드 복사 및 패키지 변경**
   - `core.kroaddy.site/oauthservice`의 모든 Java 파일을 복사
   - 패키지를 `com.labzang.api` → `com.kroaddy.api`로 변경

2. **User Service 패키지 변경**
   - `api.kroaddy.site/services/user-service`의 패키지를 `com.labzang.api` → `com.kroaddy.api`로 변경

3. **Auth Service 검토**
   - `auth-service`와 `oauth-service`의 기능 비교
   - 통합 또는 역할 분리 결정

4. **Bean 충돌 해결**
   - 동일한 Bean 이름이 있는 경우 이름 변경 또는 통합

5. **설정 파일 통합**
   - 각 서비스의 설정 파일 검토 및 통합

6. **테스트 및 검증**
   - 빌드 테스트
   - 런타임 테스트
   - 통합 테스트

## 📚 참고 자료

- Spring Boot 공식 문서: https://spring.io/projects/spring-boot
- Gradle 멀티 프로젝트: https://docs.gradle.org/current/userguide/multi_project_builds.html
- 패키지 네이밍 컨벤션: https://docs.oracle.com/javase/tutorial/java/package/namingpkgs.html

