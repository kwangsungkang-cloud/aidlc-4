# NFR Requirements — api-server

---

## 1. 성능 요구사항

### NFR-PERF-01: API 응답 시간
| 엔드포인트 유형 | 목표 응답 시간 (P95) | 비고 |
|---|---|---|
| 메뉴 조회 (GET) | ≤ 200ms | Redis 캐시 적용 |
| 주문 생성 (POST) | ≤ 500ms | DB 트랜잭션 + SSE 발행 포함 |
| 주문 상태 변경 (PATCH) | ≤ 300ms | DB 업데이트 + SSE 발행 |
| 로그인 (POST) | ≤ 500ms | bcrypt 해싱 포함 |
| 대시보드 조회 (GET) | ≤ 1000ms | 다중 JOIN 쿼리 |
| 이미지 업로드 (POST) | ≤ 3000ms | S3 업로드 포함 |

### NFR-PERF-02: 처리량
- 매장당 동시 활성 테이블: 최대 50개
- 매장당 초당 주문 요청: 최대 10 TPS
- 전체 시스템 동시 접속: 최대 2,500 (50매장 × 50테이블)
- SSE 동시 연결: 매장당 최대 10개

### NFR-PERF-03: 데이터베이스 성능
- HikariCP 연결 풀: max pool size 20
- Connection timeout: 30초
- Idle timeout: 10분
- Max lifetime: 30분
- 인덱스 전략: FK 컬럼, 조회 빈도 높은 컬럼에 인덱스 적용

### NFR-PERF-04: 캐싱
- 캐시 솔루션: Redis (AWS ElastiCache)
- 캐시 대상:
  - 메뉴 목록 (storeId 기준, TTL: 5분)
  - 카테고리 목록 (storeId 기준, TTL: 10분)
  - 매장 정보 (storeId 기준, TTL: 30분)
- 캐시 무효화: 메뉴/카테고리 CUD 시 해당 storeId 캐시 삭제
- 캐시 직렬화: JSON (Jackson)

---

## 2. 보안 요구사항

### NFR-SEC-01: 인증/인가 (SECURITY-08, SECURITY-12)
- JWT 토큰: HMAC-SHA256 서명, 16시간 만료
- 비밀번호: bcrypt (strength=10)
- 로그인 시도 제한: 5회 실패 → 30분 잠금
- Role 기반 접근 제어: TABLE_SESSION, STORE_ADMIN, SUPER_ADMIN
- 모든 엔드포인트 서버측 토큰 검증 (Spring Security 필터)

### NFR-SEC-02: 전송 보안 (SECURITY-01)
- HTTPS 필수 (TLS 1.2+)
- HTTP → HTTPS 리다이렉트
- HSTS 헤더: max-age=31536000; includeSubDomains

### NFR-SEC-03: 데이터 보호 (SECURITY-01)
- MySQL: 저장 시 암호화 (AES-256, AWS RDS 암호화)
- S3: 서버측 암호화 (SSE-S3 또는 SSE-KMS)
- Redis: 전송 중 암호화 (TLS), 저장 시 암호화

### NFR-SEC-04: 입력 검증 (SECURITY-05)
- 모든 API 파라미터 타입/길이/형식 검증 (Bean Validation)
- 파라미터화된 쿼리만 사용 (Spring Data JPA)
- 요청 본문 크기 제한: 10MB (이미지 업로드 포함)
- XSS 방지: HTML 이스케이프 처리

### NFR-SEC-05: Rate Limiting (SECURITY-11)
- API Gateway 레벨 Rate Limiting (AWS API Gateway)
- 공개 엔드포인트 (로그인): 분당 30회/IP
- 인증된 엔드포인트: 분당 100회/토큰
- 초과 시: `429 Too Many Requests`

### NFR-SEC-06: 에러 처리 (SECURITY-09, SECURITY-15)
- 프로덕션 에러 응답: 내부 정보 미노출 (스택 트레이스, DB 상세 등)
- RFC 7807 Problem Details 형식 (`application/problem+json`)
- Global Exception Handler 적용
- 모든 외부 호출 try-catch 처리

### NFR-SEC-07: 보안 헤더 (SECURITY-04)
- api-server는 REST API 전용이므로 HTML 서빙 없음
- CORS 설정: 허용 origin 명시적 지정 (customer-web, admin-web 도메인만)
- `X-Content-Type-Options: nosniff` 적용
- `Cache-Control: no-store` (인증 관련 응답)

### NFR-SEC-08: 감사 추적 (SECURITY-13, SECURITY-14)
- AdminAuditLog: append-only, 삭제 불가
- 보안 이벤트 로깅: 로그인 실패, 권한 거부, 토큰 만료
- 로그 보관: 최소 90일

---

## 3. 가용성/신뢰성 요구사항

### NFR-AVAIL-01: 서비스 가용성
- 목표: 99.5% (매장 운영 시간 기준)
- 계획된 다운타임: 주 1회 유지보수 윈도우 (새벽 2-4시)

### NFR-AVAIL-02: 장애 복구
- SSE 연결 끊김: 클라이언트 자동 재연결 (EventSource 기본 동작)
- DB 연결 실패: HikariCP 자동 재연결
- S3 업로드 실패: 에러 반환 (재시도는 클라이언트 측)
- 장바구니: 클라이언트 localStorage 보존 (서버 장애 시에도 유지)

### NFR-AVAIL-03: 헬스 체크
- Spring Boot Actuator `/actuator/health` 엔드포인트
- 체크 대상: DB 연결, Redis 연결, S3 접근
- 로드밸런서 헬스 체크 연동

---

## 4. 확장성 요구사항

### NFR-SCALE-01: 수평 확장
- Stateless 서버 설계 (JWT 기반, 서버 세션 없음)
- SSE 연결: 인스턴스별 로컬 관리 (스케일아웃 시 sticky session 또는 Redis Pub/Sub 고려)
- 캐시: Redis 중앙 집중 (인스턴스 간 공유)

### NFR-SCALE-02: 멀티테넌트
- storeId 기반 데이터 격리 (논리적 분리)
- 모든 쿼리에 storeId 조건 포함
- 인덱스: (store_id, ...) 복합 인덱스

### NFR-SCALE-03: 규모 목표
- 10~50개 매장 동시 운영
- 매장당 최대 50 테이블
- 일일 주문량: 매장당 최대 500건

---

## 5. 운영/모니터링 요구사항

### NFR-OPS-01: 구조화 로깅 (SECURITY-03)
- 프레임워크: SLF4J + Logback
- 출력 형식: JSON (CloudWatch 연동)
- 필수 필드: timestamp, level, logger, message, correlationId, storeId
- 민감 데이터 마스킹: 비밀번호, 토큰 값 로그 출력 금지
- Correlation ID: 요청별 UUID 생성 (MDC 활용)

### NFR-OPS-02: 메트릭
- Spring Boot Actuator 기본 메트릭 노출
- 커스텀 메트릭: 주문 생성 수, SSE 활성 연결 수, 로그인 실패 수
- CloudWatch 연동 준비 (Micrometer CloudWatch registry)

### NFR-OPS-03: 알림 (SECURITY-14)
- 로그인 5회 연속 실패 → 경고 로그 (CloudWatch Alarm 연동)
- 403 Forbidden 빈발 → 경고 로그
- SSE 연결 풀 포화 → 경고 로그
- DB 연결 풀 고갈 → 크리티컬 로그

---

## 6. 데이터 백업/복구 요구사항

### NFR-BACKUP-01: 백업 정책 (NFR-06)
- 일간 증분 백업: 매일 03:00 UTC (변경된 데이터만)
- 주간 전체 백업: 매주 일요일 02:00 UTC
- 백업 보관: 일간 30일, 주간 12주
- 백업 저장소: AWS S3 (다른 리전 복제 권장)

### NFR-BACKUP-02: 복구 목표
- RPO (Recovery Point Objective): 24시간 (일간 백업 기준)
- RTO (Recovery Time Objective): 4시간
- 복구 테스트: 분기 1회 권장

---

## 7. SECURITY 규칙 매핑

| SECURITY Rule | NFR 매핑 | 상태 |
|---|---|---|
| SECURITY-01 | NFR-SEC-02, NFR-SEC-03 | 설계 반영 |
| SECURITY-02 | Infrastructure Design에서 상세화 | 대기 |
| SECURITY-03 | NFR-OPS-01 | 설계 반영 |
| SECURITY-04 | NFR-SEC-07 (API 전용, N/A 부분 있음) | 설계 반영 |
| SECURITY-05 | NFR-SEC-04 | 설계 반영 |
| SECURITY-06 | Infrastructure Design에서 상세화 | 대기 |
| SECURITY-07 | Infrastructure Design에서 상세화 | 대기 |
| SECURITY-08 | NFR-SEC-01 | 설계 반영 |
| SECURITY-09 | NFR-SEC-06 | 설계 반영 |
| SECURITY-10 | tech-stack-decisions.md에서 상세화 | 설계 반영 |
| SECURITY-11 | NFR-SEC-05 | 설계 반영 |
| SECURITY-12 | NFR-SEC-01 | 설계 반영 |
| SECURITY-13 | NFR-SEC-08 | 설계 반영 |
| SECURITY-14 | NFR-OPS-03, NFR-SEC-08 | 설계 반영 |
| SECURITY-15 | NFR-SEC-06 | 설계 반영 |
