# Business Rules — api-server

---

## BR-01: 인증/인가 규칙

| 규칙 ID | 규칙 | 적용 대상 |
|---|---|---|
| BR-01-01 | 테이블 비밀번호는 bcrypt로 해싱하여 저장 | StoreTable |
| BR-01-02 | 관리자 비밀번호는 bcrypt로 해싱하여 저장 | Admin, SuperAdmin |
| BR-01-03 | JWT 토큰 만료 시간: 16시간 | 모든 인증 |
| BR-01-04 | 관리자 로그인 5회 연속 실패 시 30분 잠금 | Admin |
| BR-01-05 | 테이블 토큰은 sessionId, tableId, storeId 포함 | TableSession |
| BR-01-06 | 관리자 토큰은 adminId, storeId, role 포함 | Admin |
| BR-01-07 | 슈퍼 관리자 토큰은 superAdminId, role=SUPER_ADMIN 포함 | SuperAdmin |
| BR-01-08 | 모든 API 요청은 토큰 검증 필수 (공개 엔드포인트 제외) | 전체 |
| BR-01-09 | 관리자는 자신의 매장 데이터만 접근 가능 | Admin |
| BR-01-10 | 슈퍼 관리자는 모든 매장 데이터 접근 가능 | SuperAdmin |

---

## BR-02: 주문 규칙

| 규칙 ID | 규칙 | 적용 대상 |
|---|---|---|
| BR-02-01 | 주문은 ACTIVE 상태의 세션에서만 생성 가능 | Order |
| BR-02-02 | 주문 항목은 최소 1개 이상 필수 | Order |
| BR-02-03 | 주문 항목의 메뉴는 해당 매장 소속이어야 함 | OrderItem |
| BR-02-04 | 주문 시점의 메뉴명과 단가를 스냅샷으로 저장 | OrderItem |
| BR-02-05 | 주문 상태 전이: PENDING→PREPARING→COMPLETED (순방향만) | Order |
| BR-02-06 | 주문 삭제는 관리자만 가능 | Order |
| BR-02-07 | 주문 번호 형식: ORD-{YYYYMMDD}-{sequence} | Order |
| BR-02-08 | totalAmount = sum(quantity * unitPrice) | Order |
| BR-02-09 | 수량은 1 이상이어야 함 | OrderItem |

---

## BR-03: 세션 관리 규칙

| 규칙 ID | 규칙 | 적용 대상 |
|---|---|---|
| BR-03-01 | 테이블당 활성 세션은 최대 1개 | TableSession |
| BR-03-02 | 세션 만료 시간: 생성 시점 + 16시간 | TableSession |
| BR-03-03 | 만료된 세션은 자동으로 COMPLETED 처리 | TableSession |
| BR-03-04 | 이용 완료 시 미완료 주문은 COMPLETED로 변경 | Order |
| BR-03-05 | 이용 완료 후 새 주문 시 새 세션 자동 생성 | TableSession |
| BR-03-06 | 고객은 현재 ACTIVE 세션의 주문만 조회 가능 | Order |

---

## BR-04: 메뉴 관리 규칙

| 규칙 ID | 규칙 | 적용 대상 |
|---|---|---|
| BR-04-01 | 메뉴명은 필수 (max 100자) | Menu |
| BR-04-02 | 가격은 필수, 0 초과 10,000,000 이하 | Menu |
| BR-04-03 | 카테고리는 필수, 같은 매장 소속이어야 함 | Menu |
| BR-04-04 | 이미지는 선택, S3에 업로드 후 URL 저장 | Menu |
| BR-04-05 | 메뉴 삭제 시 기존 주문의 스냅샷 데이터는 유지 | OrderItem |
| BR-04-06 | displayOrder는 카테고리 내에서 유니크 권장 | Menu |
| BR-04-07 | 카테고리명은 필수 (max 50자) | Category |

---

## BR-05: 관리자 관리 규칙

| 규칙 ID | 규칙 | 적용 대상 |
|---|---|---|
| BR-05-01 | (storeId, username) 조합은 유니크 | Admin |
| BR-05-02 | 관리자 생성/삭제 시 AdminAuditLog 자동 기록 | AdminAuditLog |
| BR-05-03 | AdminAuditLog는 삭제 불가 (append-only) | AdminAuditLog |
| BR-05-04 | 삭제된 관리자의 username은 스냅샷으로 이력에 보존 | AdminAuditLog |
| BR-05-05 | 슈퍼 관리자만 관리자 계정 CRUD 가능 | Admin |

---

## BR-06: 데이터 검증 규칙

| 규칙 ID | 규칙 | 적용 대상 |
|---|---|---|
| BR-06-01 | 모든 문자열 입력은 max length 검증 | 전체 |
| BR-06-02 | 모든 숫자 입력은 범위 검증 | 전체 |
| BR-06-03 | FK 참조는 존재 여부 검증 | 전체 |
| BR-06-04 | 매장 간 데이터 격리 (storeId 기반) | 전체 |
| BR-06-05 | 이미지 업로드: 허용 형식 (jpg, png, webp), 최대 5MB | FileStorage |
| BR-06-06 | SQL injection 방지: 파라미터화된 쿼리만 사용 | 전체 |
