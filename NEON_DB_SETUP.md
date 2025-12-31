# Neon DB 연결 설정 가이드

## 🔍 Neon DB 연결 정보 확인 방법

### 1. Neon 대시보드 접속
1. [Neon Console](https://console.neon.tech)에 로그인
2. 프로젝트 선택
3. 데이터베이스 선택

### 2. Connection String 확인
Neon 대시보드에서 **Connection String**을 확인하세요. 예시:

```
postgresql://neondb_owner:npg_g3QcJ0f@ep-square-term-a125uxd-pooler.ap-southeast-1.aws.neon.tech:5432/neondb?sslmode=require
```

### 3. 연결 정보 파싱
Connection String에서 다음 정보를 추출:

- **Host**: `ep-square-term-a125uxd-pooler.ap-southeast-1.aws.neon.tech`
- **Port**: `5432` (기본값)
- **Database**: `neondb`
- **Username**: `neondb_owner`
- **Password**: `npg_g3QcJ0f`

## 📝 .env 파일 생성

`api.kroaddy.site/.env` 파일을 생성하고 다음 형식으로 작성:

```env
# Spring DataSource Configuration (Neon DB)
SPRING_DATASOURCE_URL=jdbc:postgresql://ep-square-term-a125uxd-pooler.ap-southeast-1.aws.neon.tech:5432/neondb?sslmode=require
SPRING_DATASOURCE_USERNAME=neondb_owner
SPRING_DATASOURCE_PASSWORD=npg_g3QcJ0f
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
```

## ⚠️ 주의사항

### 1. 예시 코드의 오타 수정
예시에 오타가 있습니다:
- ❌ `org.postesql.Driver` (잘못됨)
- ✅ `org.postgresql.Driver` (올바름)

### 2. Connection String 형식
Neon DB의 Connection String은 다음과 같은 형식입니다:
```
postgresql://[username]:[password]@[host]:[port]/[database]?sslmode=require
```

JDBC URL로 변환:
```
jdbc:postgresql://[host]:[port]/[database]?sslmode=require
```

### 3. 보안
- `.env` 파일은 `.gitignore`에 추가되어 있어야 합니다
- 실제 비밀번호는 절대 공유하지 마세요
- 프로덕션에서는 환경 변수나 시크릿 관리 서비스를 사용하세요

## 🔧 application.yaml 설정

`services/user-service/src/main/resources/application.yaml`에 다음 설정을 추가:

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    driver-class-name: ${SPRING_DATASOURCE_DRIVER_CLASS_NAME}
    hikari:
      maximum-pool-size: ${SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE:10}
      minimum-idle: ${SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE:5}
      connection-timeout: ${SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT:30000}
  
  jpa:
    hibernate:
      ddl-auto: ${SPRING_JPA_HIBERNATE_DDL_AUTO:update}
    show-sql: ${SPRING_JPA_SHOW_SQL:false}
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

## 📦 PostgreSQL 드라이버 의존성 확인

`services/user-service/build.gradle`에 PostgreSQL 드라이버가 포함되어 있는지 확인:

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    runtimeOnly 'org.postgresql:postgresql'  // 추가 필요할 수 있음
}
```

## 🚀 사용 방법

1. `.env.example`을 복사하여 `.env` 파일 생성
2. Neon DB 대시보드에서 실제 연결 정보 입력
3. 애플리케이션 실행 시 환경 변수 자동 로드

