# 테이블오더 서비스 — Unit 의존성

---

## Unit 간 의존성 매트릭스

| Unit | api-server | customer-web | admin-web |
|---|---|---|---|
| api-server | - | - | - |
| customer-web | ● REST API | - | - |
| admin-web | ● REST API + SSE | - | - |

● = 의존

---

## 의존성 방향

```
customer-web ──REST──> api-server <──REST+SSE── admin-web
```

- **api-server**: 독립적 (다른 Unit에 의존하지 않음)
- **customer-web**: api-server의 REST API에 의존
- **admin-web**: api-server의 REST API + SSE에 의존
- **customer-web ↔ admin-web**: 직접 의존 없음 (독립적)

---

## 의존성 상세

### customer-web → api-server
| API 그룹 | 용도 |
|---|---|
| /api/table/auth | 테이블 자동 로그인 |
| /api/customer/menus | 메뉴 조회 |
| /api/customer/orders | 주문 생성/조회 |

### admin-web → api-server
| API 그룹 | 용도 |
|---|---|
| /api/admin/auth | 관리자 로그인 |
| /api/admin/orders | 주문 관리/상태 변경 |
| /api/admin/tables | 테이블 관리/세션 관리 |
| /api/admin/menus | 메뉴 CRUD |
| /api/admin/sse | 실시간 주문 이벤트 |
| /api/super-admin | 계정/이력 관리 |

---

## 순환 의존성 검증
- 순환 의존성 없음 ✅
- 모든 의존성이 프론트엔드 → 백엔드 단방향
