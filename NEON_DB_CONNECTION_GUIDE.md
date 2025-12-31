# Neon DB 연결 정보 확인 가이드

## 📍 각 환경 변수 확인 위치

### 1. SPRING_DATASOURCE_URL

**확인 위치**: Neon 대시보드 → 프로젝트 → 데이터베이스 → **Connection String**

**절차**:
1. [Neon Console](https://console.neon.tech) 로그인
2. 프로젝트 선택
3. 데이터베이스 선택
4. **"Connection Details"** 또는 **"Connection String"** 섹션 확인
5. Connection String 예시:
   ```
   postgresql://neondb_owner:npg_g3QcJ0f@ep-square-term-a125uxd-pooler.ap-southeast-1.aws.neon.tech:5432/neondb?sslmode=require
   ```

**변환 방법**:
- Connection String: `postgresql://user:pass@host:port/db?sslmode=require`
- JDBC URL: `jdbc:postgresql://host:port/db?sslmode=require`
- **예시**: `jdbc:postgresql://ep-square-term-a125uxd-pooler.ap-southeast-1.aws.neon.tech:5432/neondb?sslmode=require`

**Connection String에서 추출**:
- `postgresql://` → `jdbc:postgresql://`로 변경
- `@` 앞의 `username:password` 부분 제거
- `@` 뒤의 `host:port/database?sslmode=require` 부분 사용

---

### 2. SPRING_DATASOURCE_USERNAME

**확인 위치**: Neon 대시보드 → 프로젝트 → 데이터베이스 → **Connection String** 또는 **Database Users**

**절차**:
1. **방법 1**: Connection String에서 확인
   - Connection String: `postgresql://**neondb_owner**:password@host...`
   - `://` 뒤, `:` 앞 부분이 username
   - 예시: `neondb_owner`

2. **방법 2**: Database Users 섹션에서 확인
   - 프로젝트 → **"Database Users"** 또는 **"Users"** 탭
   - 기본 사용자명 확인 (보통 `neondb_owner` 또는 프로젝트명_owner)

**예시 값**:
```
SPRING_DATASOURCE_USERNAME=neondb_owner
```

---

### 3. SPRING_DATASOURCE_PASSWORD

**확인 위치**: Neon 대시보드 → 프로젝트 → 데이터베이스 → **Connection String** (처음 생성 시에만 표시)

**절차**:
1. **방법 1**: Connection String에서 확인 (처음 생성 시)
   - Connection String: `postgresql://username:**npg_g3QcJ0f**@host...`
   - `:` 뒤, `@` 앞 부분이 password
   - ⚠️ **주의**: Connection String은 처음 생성 시에만 전체 표시됨

2. **방법 2**: 비밀번호 재설정
   - 프로젝트 → **"Database Users"** → 사용자 선택
   - **"Reset Password"** 클릭
   - 새 비밀번호 생성 및 복사

**예시 값**:
```
SPRING_DATASOURCE_PASSWORD=npg_g3QcJ0f
```

---

### 4. SPRING_DATASOURCE_DRIVER_CLASS_NAME

**확인 위치**: ❌ Neon DB에서 확인 불가 - **고정값**

**값**: 
```
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
```

**설명**:
- PostgreSQL JDBC 드라이버의 클래스명
- Neon DB와 무관하며 항상 동일한 값
- ⚠️ **오타 주의**: `org.postesql.Driver` (X) → `org.postgresql.Driver` (O)

---

## 🔍 Neon 대시보드 화면별 확인 방법

### 화면 1: Connection String 확인
```
Neon Console
  └─ 프로젝트 선택
      └─ 데이터베이스 선택
          └─ "Connection Details" 또는 "Connection String" 섹션
              └─ [Connection String 표시]
                  postgresql://username:password@host:port/database?sslmode=require
```

### 화면 2: Database Users 확인
```
Neon Console
  └─ 프로젝트 선택
      └─ "Database Users" 또는 "Users" 탭
          └─ [사용자 목록]
              └─ 사용자명 확인 (username)
              └─ "Reset Password" 클릭 (password 재설정)
```

### 화면 3: Database 정보 확인
```
Neon Console
  └─ 프로젝트 선택
      └─ 데이터베이스 선택
          └─ "Settings" 또는 "Details" 탭
              └─ Database name 확인
              └─ Host 확인
              └─ Port 확인 (기본: 5432)
```

---

## 📝 Connection String 파싱 예시

**Connection String**:
```
postgresql://neondb_owner:npg_g3QcJ0f@ep-square-term-a125uxd-pooler.ap-southeast-1.aws.neon.tech:5432/neondb?sslmode=require
```

**파싱 결과**:
- **Username**: `neondb_owner` (:// 뒤, : 앞)
- **Password**: `npg_g3QcJ0f` (: 뒤, @ 앞)
- **Host**: `ep-square-term-a125uxd-pooler.ap-southeast-1.aws.neon.tech` (@ 뒤, : 앞)
- **Port**: `5432` (첫 번째 : 뒤, / 앞)
- **Database**: `neondb` (마지막 / 뒤, ? 앞)

**환경 변수 변환**:
```env
SPRING_DATASOURCE_URL=jdbc:postgresql://ep-square-term-a125uxd-pooler.ap-southeast-1.aws.neon.tech:5432/neondb?sslmode=require
SPRING_DATASOURCE_USERNAME=neondb_owner
SPRING_DATASOURCE_PASSWORD=npg_g3QcJ0f
SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver
```

---

## ⚠️ 중요 사항

### 1. Connection String 보안
- Connection String은 **처음 생성 시에만 전체 표시**됩니다
- 비밀번호를 잊어버린 경우 **"Reset Password"**로 재설정하세요

### 2. Pooler vs Direct Connection
- Neon은 **Pooler**와 **Direct** 두 가지 연결 방식 제공
- Pooler: `-pooler`가 포함된 host 사용 (권장)
- Direct: `-pooler`가 없는 host 사용

### 3. SSL 모드
- Neon DB는 SSL 필수: `?sslmode=require` 항상 포함

---

## 🎯 빠른 확인 체크리스트

- [ ] Neon Console 로그인
- [ ] 프로젝트 선택
- [ ] 데이터베이스 선택
- [ ] Connection String 복사
- [ ] Username 추출 (:// 뒤, : 앞)
- [ ] Password 추출 (: 뒤, @ 앞) 또는 재설정
- [ ] Host 추출 (@ 뒤, : 앞)
- [ ] Port 확인 (기본: 5432)
- [ ] Database name 추출 (마지막 / 뒤, ? 앞)
- [ ] JDBC URL 형식으로 변환

