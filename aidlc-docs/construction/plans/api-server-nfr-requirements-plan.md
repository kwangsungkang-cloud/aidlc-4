# NFR Requirements Plan — Unit 1: api-server

## 평가 계획

### Phase 1: 기존 NFR 분석
- [x] requirements.md의 NFR-01~NFR-06 분석
- [x] Functional Design 산출물 기반 NFR 영향도 평가

### Phase 2: NFR 상세 요구사항 도출
- [x] 성능 요구사항 상세화 (응답 시간, 처리량, 동시 접속)
- [x] 보안 요구사항 상세화 (SECURITY-01~15 매핑)
- [x] 가용성/신뢰성 요구사항 상세화
- [x] 확장성 요구사항 상세화
- [x] 운영/모니터링 요구사항 도출
- [x] 데이터 백업/복구 요구사항 상세화

### Phase 3: 기술 스택 결정
- [x] Spring Boot 버전 및 주요 라이브러리 선정
- [x] 데이터베이스 접근 기술 결정 (JPA 설정)
- [x] 보안 프레임워크 결정 (Spring Security 설정)
- [x] 빌드/패키징 도구 결정
- [x] 테스트 프레임워크 결정

### Phase 4: NFR 산출물 생성
- [x] nfr-requirements.md 생성
- [x] tech-stack-decisions.md 생성

---

## 명확화 질문

### Question 1: Spring Boot 버전 및 언어 선택
api-server의 구현 언어와 Spring Boot 버전을 결정해야 합니다.

A) Java 21 + Spring Boot 3.4.x (최신 LTS, Virtual Threads 지원)
B) Java 17 + Spring Boot 3.2.x (안정적 LTS)
C) Kotlin + Spring Boot 3.4.x (코루틴 기반 비동기)
D) Kotlin + Spring Boot 3.2.x
X) 기타 (아래에 설명)

[Answer]: B

### Question 2: 데이터베이스 연결 풀 및 성능 설정
MySQL 연결 풀 크기와 성능 관련 설정을 결정해야 합니다.

A) 기본 설정 (HikariCP 기본값, max pool size 10)
B) 중규모 최적화 (max pool size 20, connection timeout 30s, idle timeout 10min)
C) 고성능 설정 (max pool size 30+, 커스텀 튜닝)
X) 기타 (아래에 설명)

[Answer]: B

### Question 3: 로깅 및 모니터링 수준
api-server의 로깅 및 모니터링 수준을 결정해야 합니다. (SECURITY-03, SECURITY-14 관련)

A) 기본 로깅 (SLF4J + Logback, 콘솔/파일 출력, 구조화 로깅 없음)
B) 구조화 로깅 (JSON 포맷, correlation ID, CloudWatch 연동 준비)
C) 풀 옵저버빌리티 (구조화 로깅 + Micrometer 메트릭 + 분산 추적)
X) 기타 (아래에 설명)

[Answer]: B

### Question 4: API Rate Limiting 전략
공개 엔드포인트(로그인 등)의 Rate Limiting 전략을 결정해야 합니다. (SECURITY-11 관련)

A) 애플리케이션 레벨 (Spring 필터 기반, 인메모리 카운터)
B) 애플리케이션 레벨 + Redis (분산 환경 대응)
C) API Gateway 레벨 (AWS API Gateway throttling)
D) 로그인 시도 제한만 (이미 설계된 5회 잠금으로 충분)
X) 기타 (아래에 설명)

[Answer]: C

### Question 5: 캐싱 전략
메뉴 조회 등 읽기 빈도가 높은 API의 캐싱 전략을 결정해야 합니다.

A) 캐싱 없음 (MVP에서는 DB 직접 조회로 충분)
B) 애플리케이션 레벨 캐시 (Spring Cache + Caffeine, 로컬 인메모리)
C) 분산 캐시 (Redis/ElastiCache)
X) 기타 (아래에 설명)

[Answer]: C

### Question 6: 에러 처리 및 응답 표준화
API 에러 응답 형식을 결정해야 합니다. (SECURITY-09, SECURITY-15 관련)

A) 간단한 커스텀 형식 (`{ "error": "message", "code": "ERROR_CODE" }`)
B) RFC 7807 Problem Details 표준 (`application/problem+json`)
C) Spring Boot 기본 에러 응답 형식 활용
X) 기타 (아래에 설명)

[Answer]: B

### Question 7: 테스트 커버리지 목표
api-server의 테스트 커버리지 목표를 결정해야 합니다.

A) 핵심 비즈니스 로직만 (서비스 레이어 위주, 60%+)
B) 표준 커버리지 (서비스 + 컨트롤러 + 리포지토리, 70%+)
C) 높은 커버리지 (전체 레이어, 80%+, 통합 테스트 포함)
X) 기타 (아래에 설명)

[Answer]: B or C
