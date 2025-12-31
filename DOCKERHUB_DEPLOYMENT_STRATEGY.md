# Docker Hub를 이용한 EC2 배포 전략

**작성일**: 2024년 12월 30일  
**프로젝트**: Kroaddy API Gateway - Docker Hub 기반 배포 전략

---

## 📋 목차

1. [전략 개요](#1-전략-개요)
2. [배포 아키텍처](#2-배포-아키텍처)
3. [필요한 준비사항](#3-필요한-준비사항)
4. [배포 프로세스](#4-배포-프로세스)
5. [구현 방향](#5-구현-방향)
6. [주요 고려사항](#6-주요-고려사항)
7. [장단점 분석](#7-장단점-분석)

---

## 1. 전략 개요

### 1.1 무엇을 하는가?

Spring Boot 애플리케이션을 Docker 이미지로 빌드하여 Docker Hub에 푸시하고, EC2 인스턴스에서 해당 이미지를 pull하여 실행하는 방식의 CI/CD 파이프라인을 구축합니다.

### 1.2 핵심 개념

- **Docker Hub**: Docker 이미지 저장소 (공개/비공개 리포지토리)
- **GitHub Actions**: 코드 변경 시 자동으로 이미지 빌드 및 푸시
- **EC2**: 프로덕션 환경에서 Docker Hub에서 이미지를 pull하여 실행

### 1.3 왜 이 방식을 선택하는가?

- ✅ **빠른 배포**: EC2에서 이미지만 pull하면 되므로 배포 시간 단축
- ✅ **버전 관리**: Docker Hub의 태그 시스템으로 버전 관리 용이
- ✅ **롤백 용이**: 이전 태그로 즉시 롤백 가능
- ✅ **빌드 부하 분산**: EC2에서 빌드하지 않고 Docker Hub에서 받아옴
- ✅ **캐시 활용**: Docker 레이어 캐시로 빌드 시간 단축

---

## 2. 배포 아키텍처

### 2.1 전체 흐름도

```
개발자
    ↓ (코드 수정 및 push)
GitHub Repository
    ↓ (트리거: main/develop 브랜치 push)
GitHub Actions
    ├─→ 코드 체크아웃
    ├─→ JDK 21 설정
    ├─→ Gradle 빌드 (JAR 생성)
    ├─→ Docker 이미지 빌드
    └─→ Docker Hub에 푸시
            ↓
        Docker Hub
            ├─→ 이미지 저장
            ├─→ 태그 관리 (latest, main, develop 등)
            └─→ 버전 히스토리
                    ↓
                EC2 Instance
                    ├─→ Docker Hub에서 이미지 pull
                    ├─→ docker-compose로 컨테이너 실행
                    └─→ Spring Boot 애플리케이션 실행 (Port 8080)
```

### 2.2 컴포넌트별 역할

| 컴포넌트 | 역할 | 책임 |
|---------|------|------|
| **GitHub Repository** | 코드 저장소 | 소스 코드 관리, 버전 관리 |
| **GitHub Actions** | CI/CD 파이프라인 | 자동 빌드, 이미지 푸시 |
| **Docker Hub** | 이미지 저장소 | Docker 이미지 저장, 배포 |
| **EC2 Instance** | 프로덕션 환경 | 이미지 pull, 컨테이너 실행 |

---

## 3. 필요한 준비사항

### 3.1 계정 및 리소스

1. **Docker Hub 계정**
   - 무료 계정 생성 가능
   - 리포지토리 생성 (Public 또는 Private)
   - Access Token 생성 (GitHub Actions 인증용)

2. **GitHub Repository**
   - 코드 저장소
   - GitHub Actions 활성화
   - Secrets 설정 (Docker Hub 인증 정보)

3. **EC2 인스턴스*  
   - Ubuntu 24.04 LTS 권장
   - 최소 사양: 2GB RAM, 10GB 스토리지
   - Docker 및 Docker Compose 설치 필요
   - 보안 그룹: 8080 포트 오픈

### 3.2 필요한 설정

#### GitHub Secrets
- `DOCKERHUB_USERNAME`: Docker Hub 사용자명
- `DOCKERHUB_TOKEN`: Docker Hub Access Token

#### EC2 환경변수
- `.env` 파일: 데이터베이스, Redis, JWT, OAuth 등 설정
- `docker-compose.prod.yaml`: Docker Hub 이미지 사용 설정

---

## 4. 배포 프로세스

### 4.1 개발 단계

1. **코드 수정**
   - 로컬에서 코드 수정
   - 테스트 실행

2. **커밋 및 푸시**
   ```bash
   git add .
   git commit -m "Update feature"
   git push origin main  # 또는 develop
   ```

### 4.2 CI/CD 단계 (GitHub Actions)

1. **트리거**
   - `main` 또는 `develop` 브랜치에 push 시 자동 실행
   - 또는 수동 실행 (workflow_dispatch)

2. **빌드 프로세스**
   - 코드 체크아웃
   - JDK 21 환경 설정
   - Gradle로 JAR 파일 빌드
   - Docker 이미지 빌드 (Dockerfile 사용)
   - Docker Hub에 로그인
   - 이미지에 태그 부여 (latest, 브랜치명, 커밋 SHA 등)
   - Docker Hub에 푸시

3. **태그 전략**
   - `latest`: 기본 브랜치 (main)의 최신 버전
   - `main`: main 브랜치 버전
   - `develop`: develop 브랜치 버전
   - `main-<sha>`: 특정 커밋 버전
   - `develop-<sha>`: 특정 커밋 버전

### 4.3 배포 단계 (EC2)

#### 방법 1: 수동 배포
1. **SSH 접속**
   ```bash
   ssh -i ~/.ssh/spring.pem ubuntu@ec2-ip
   ```

2. **이미지 pull**
   ```bash
   docker pull username/kroaddy-api-gateway:latest
   ```

3. **컨테이너 재시작**
   ```bash
   docker-compose -f docker-compose.prod.yaml down
   docker-compose -f docker-compose.prod.yaml up -d
   ```

#### 방법 2: 자동화 스크립트
- 배포 스크립트 작성 (`deploy-ec2.sh`)
- EC2에 SSH 접속하여 자동으로 pull 및 재시작
- 환경변수로 Docker Hub 사용자명, 이미지 태그 지정

#### 방법 3: 완전 자동화 (향후)
- GitHub Actions에서 EC2에 SSH 접속
- 이미지 pull 및 컨테이너 재시작까지 자동화
- 헬스체크 포함

---

## 5. 구현 방향

### 5.1 GitHub Actions Workflow

**파일 위치**: `.github/workflows/dockerhub-deploy.yml`

**주요 단계**:
1. Checkout code
2. Set up JDK 21
3. Set up Docker Buildx (멀티 플랫폼 빌드 지원)
4. Login to Docker Hub (Secrets 사용)
5. Extract metadata (태그 자동 생성)
6. Build and push Docker image
7. (선택) Notify deployment

**태그 전략**:
- 브랜치명 기반 태그
- 커밋 SHA 기반 태그
- latest 태그 (기본 브랜치만)
- Semantic versioning 지원 (향후)

### 5.2 Dockerfile 최적화

**현재 구조**:
- Multi-stage build (빌드 단계 + 실행 단계)
- Gradle 빌드
- JRE만 포함하여 이미지 크기 최소화

**개선 방향**:
- Docker 레이어 캐시 최적화
- .dockerignore로 불필요한 파일 제외
- 빌드 시간 단축

### 5.3 Docker Compose 설정

**프로덕션용 설정** (`docker-compose.prod.yaml`):
- `build` 대신 `image` 사용 (Docker Hub 이미지)
- 환경변수 파일 (.env) 참조
- 헬스체크 설정
- 로깅 설정
- 재시작 정책 (unless-stopped)

### 5.4 EC2 배포 스크립트

**기능**:
- Docker Hub에서 이미지 pull
- 기존 컨테이너 중지
- 새 컨테이너 시작
- 헬스체크
- 로그 확인

**사용법**:
```bash
EC2_HOST=ec2-ip \
DOCKERHUB_USERNAME=username \
IMAGE_TAG=latest \
./deploy-ec2.sh
```

---

## 6. 주요 고려사항

### 6.1 보안

1. **Docker Hub Access Token**
   - GitHub Secrets에만 저장
   - 정기적으로 갱신
   - 최소 권한 원칙 (Read, Write만)

2. **환경변수 관리**
   - `.env` 파일은 Git에 커밋하지 않음
   - EC2에서만 관리
   - 프로덕션에서는 AWS Secrets Manager 고려

3. **이미지 보안**
   - Private 리포지토리 사용 고려
   - 이미지 스캔 도구 활용
   - 최신 베이스 이미지 사용

### 6.2 버전 관리

1. **태그 전략**
   - `latest` 태그는 편리하지만 위험할 수 있음
   - 프로덕션에서는 구체적인 버전 태그 사용 권장
   - Semantic versioning 도입 고려

2. **롤백 전략**
   - 이전 태그로 쉽게 롤백 가능
   - 롤백 스크립트 준비
   - 데이터베이스 마이그레이션 고려

### 6.3 모니터링

1. **배포 상태 확인**
   - GitHub Actions 로그
   - Docker Hub 이미지 업로드 확인
   - EC2 컨테이너 상태 확인

2. **애플리케이션 모니터링**
   - 헬스체크 엔드포인트
   - 로그 모니터링
   - 메트릭 수집 (향후)

### 6.4 성능 최적화

1. **이미지 크기**
   - Multi-stage build로 최소화
   - 불필요한 파일 제외
   - Alpine Linux 기반 이미지 고려

2. **빌드 시간**
   - Docker 레이어 캐시 활용
   - Gradle 빌드 캐시 활용
   - 병렬 빌드 고려

---

## 7. 장단점 분석

### 7.1 장점

✅ **빠른 배포**
- EC2에서 빌드하지 않고 이미지만 pull
- 배포 시간 대폭 단축

✅ **버전 관리 용이**
- Docker Hub의 태그 시스템 활용
- 이전 버전으로 쉽게 롤백

✅ **빌드 부하 분산**
- EC2 리소스를 빌드에 사용하지 않음
- GitHub Actions에서 빌드 처리

✅ **일관성 보장**
- 동일한 이미지를 모든 환경에서 사용
- "내 컴퓨터에서는 되는데" 문제 해결

✅ **캐시 활용**
- Docker 레이어 캐시로 빌드 시간 단축
- 불필요한 재빌드 방지

### 7.2 단점

❌ **Docker Hub 의존성**
- Docker Hub 장애 시 배포 불가
- Private 리포지토리는 유료 플랜 필요

❌ **이미지 크기**
- Docker 이미지가 상대적으로 큼
- 네트워크 대역폭 사용

❌ **초기 설정 복잡도**
- Docker Hub 계정, GitHub Secrets 등 설정 필요
- EC2 초기 설정 필요

❌ **비용**
- Docker Hub Private 리포지토리는 유료
- Public 리포지토리는 무료이지만 공개됨

---

## 8. 향후 개선 방향

### 8.1 단기 개선

- [ ] 자동 배포 스크립트 완성
- [ ] 헬스체크 자동화
- [ ] 롤백 스크립트 작성
- [ ] 배포 알림 설정 (Slack, Email 등)

### 8.2 중기 개선

- [ ] 완전 자동화 (GitHub Actions → EC2 자동 배포)
- [ ] 다중 환경 지원 (staging, production)
- [ ] Blue-Green 배포 전략
- [ ] 모니터링 도구 연동

### 8.3 장기 개선

- [ ] Kubernetes로 마이그레이션
- [ ] AWS ECS/EKS 고려
- [ ] 이미지 스캔 자동화
- [ ] CI/CD 파이프라인 확장

---

## 9. 참고 자료

### 9.1 공식 문서

- [Docker Hub Documentation](https://docs.docker.com/docker-hub/)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Docker Compose Documentation](https://docs.docker.com/compose/)

### 9.2 관련 도구

- **Docker Buildx**: 멀티 플랫폼 빌드
- **Docker Metadata Action**: 태그 자동 생성
- **Docker Login Action**: Docker Hub 인증

---

## 10. 체크리스트

### 사전 준비
- [ ] Docker Hub 계정 생성
- [ ] Docker Hub 리포지토리 생성
- [ ] Docker Hub Access Token 생성
- [ ] GitHub Secrets 설정
- [ ] EC2 인스턴스 준비
- [ ] EC2에 Docker 설치

### 구현 단계
- [ ] GitHub Actions Workflow 작성
- [ ] Dockerfile 최적화
- [ ] docker-compose.prod.yaml 작성
- [ ] 배포 스크립트 작성
- [ ] 환경변수 파일 준비

### 테스트 단계
- [ ] 로컬에서 Docker 이미지 빌드 테스트
- [ ] Docker Hub에 푸시 테스트
- [ ] EC2에서 이미지 pull 테스트
- [ ] 컨테이너 실행 테스트
- [ ] 헬스체크 확인

### 배포 단계
- [ ] 첫 배포 실행
- [ ] 로그 확인
- [ ] 애플리케이션 동작 확인
- [ ] 롤백 테스트

---

**작성자**: AI Assistant  
**최종 수정일**: 2024-12-30  
**상태**: 전략 문서 (구현 전)

