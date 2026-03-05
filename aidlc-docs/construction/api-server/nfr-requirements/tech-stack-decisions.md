# Tech Stack Decisions — api-server

---

## 1. 핵심 프레임워크

| 기술 | 버전 | 선택 근거 |
|---|---|---|
| Java | 17 (LTS) | 안정적 LTS, 광범위한 라이브러리 호환성 |
| Spring Boot | 3.2.x | 안정적 릴리스, Java 17 완전 지원, Spring Security 6.x 포함 |
| Gradle | 8.x | Kotlin DSL, 빌드 캐시, 의존성 관리 |

---

## 2. 데이터 접근

| 기술 | 버전 | 용도 |
|---|---|---|
| Spring Data JPA | 3.2.x (Boot 포함) | ORM, Repository 패턴 |
| Hibernate | 6.4.x (Boot 포함) | JPA 구현체 |
| HikariCP | 5.x (Boot 포함) | DB 연결 풀 |
| MySQL Connector/J | 8.3.x | MySQL JDBC 드라이버 |
| Flyway | 10.x | DB 마이그레이션 관리 |

**HikariCP 설정**:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

---

## 3. 캐싱

| 기술 | 버전 | 용도 |
|---|---|---|
| Spring Data Redis | 3.2.x (Boot 포함) | Redis 클라이언트 |
| Lettuce | 6.3.x (Boot 포함) | Redis 비동기 클라이언트 |
| AWS ElastiCache (Redis) | 7.x | 분산 캐시 서비스 |

**캐시 설정**:
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST}
      port: 6379
      ssl:
        enabled: true
  cache:
    type: redis
    redis:
      time-to-live: 300000  # 기본 5분
```

---

## 4. 보안

| 기술 | 버전 | 용도 |
|---|---|---|
| Spring Security | 6.2.x (Boot 포함) | 인증/인가 프레임워크 |
| jjwt (io.jsonwebtoken) | 0.12.x | JWT 생성/검증 |
| Spring Security Crypto | 6.2.x | bcrypt 해싱 |

**JWT 설정**:
- 알고리즘: HMAC-SHA256
- 시크릿 키: 환경 변수 (`JWT_SECRET`)
- 만료: 16시간 (57,600초)

---

## 5. 파일 스토리지

| 기술 | 버전 | 용도 |
|---|---|---|
| AWS SDK for Java v2 | 2.25.x | S3 클라이언트 |
| S3 Transfer Manager | 2.25.x | 이미지 업로드/삭제 |

**S3 설정**:
- 버킷: `{project}-menu-images-{env}`
- 경로: `menus/{storeId}/{UUID}.{ext}`
- 암호화: SSE-S3
- 공개 접근: 차단 (Pre-signed URL 또는 CloudFront 경유)

---

## 6. 실시간 통신

| 기술 | 용도 |
|---|---|
| Spring MVC SseEmitter | SSE 서버 구현 |
| CopyOnWriteArrayList | Thread-safe 연결 풀 |

---

## 7. 로깅/모니터링

| 기술 | 버전 | 용도 |
|---|---|---|
| SLF4J | 2.0.x (Boot 포함) | 로깅 API |
| Logback | 1.4.x (Boot 포함) | 로깅 구현체 |
| Logstash Logback Encoder | 7.4.x | JSON 구조화 로깅 |
| Spring Boot Actuator | 3.2.x | 헬스 체크, 메트릭 |
| Micrometer | 1.12.x (Boot 포함) | 메트릭 수집 |

**Logback 설정** (logback-spring.xml):
```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
  <includeMdcKeyName>correlationId</includeMdcKeyName>
  <includeMdcKeyName>storeId</includeMdcKeyName>
</encoder>
```

---

## 8. 입력 검증

| 기술 | 버전 | 용도 |
|---|---|---|
| Jakarta Bean Validation | 3.0 (Boot 포함) | 어노테이션 기반 검증 |
| Hibernate Validator | 8.0.x (Boot 포함) | Bean Validation 구현체 |

---

## 9. 에러 처리

| 기술 | 용도 |
|---|---|
| RFC 7807 Problem Details | 표준 에러 응답 형식 |
| Spring 6 ProblemDetail | RFC 7807 네이티브 지원 |
| @RestControllerAdvice | Global Exception Handler |

**에러 응답 예시**:
```json
{
  "type": "https://api.tableorder.com/errors/validation",
  "title": "입력 검증 실패",
  "status": 400,
  "detail": "메뉴명은 필수입니다",
  "instance": "/api/admin/menus"
}
```

---

## 10. 테스트

| 기술 | 버전 | 용도 |
|---|---|---|
| JUnit 5 | 5.10.x (Boot 포함) | 단위 테스트 |
| Mockito | 5.x (Boot 포함) | 모킹 |
| Spring Boot Test | 3.2.x | 통합 테스트 |
| Testcontainers | 1.19.x | MySQL/Redis 통합 테스트 |
| JaCoCo | 0.8.x | 코드 커버리지 |

**커버리지 목표**: 70%+ (서비스 + 컨트롤러 + 리포지토리)
- 핵심 비즈니스 로직 (주문, 세션, 인증): 통합 테스트 포함
- Testcontainers로 실제 MySQL/Redis 환경 테스트

---

## 11. 빌드/패키징

| 항목 | 결정 |
|---|---|
| 빌드 도구 | Gradle 8.x (Kotlin DSL) |
| 패키징 | Spring Boot Fat JAR |
| 컨테이너 | Docker (Eclipse Temurin 17-jre 베이스) |
| 의존성 잠금 | Gradle dependency locking 활성화 |

**Dockerfile**:
```dockerfile
FROM eclipse-temurin:17-jre-jammy
COPY build/libs/api-server-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 12. 소프트웨어 공급망 보안 (SECURITY-10)

| 항목 | 결정 |
|---|---|
| 의존성 잠금 | Gradle dependency locking (`gradle.lockfile`) |
| 취약점 스캔 | OWASP Dependency-Check Gradle 플러그인 |
| 미사용 의존성 | Gradle lint 플러그인으로 감지 |
| 레지스트리 | Maven Central (공식) |
| Docker 이미지 | 태그 고정 (`:17-jre-jammy`, `latest` 사용 금지) |
