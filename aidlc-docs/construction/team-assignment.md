# Construction 4인 팀 분배 — 공통 인프라 완성 후

---

## 공통 인프라 (완성됨 ✅)

이미 생성된 코드:
- `api-server/build.gradle.kts` — 전체 의존성
- `api-server/src/main/resources/` — application.yml, logback-spring.xml, docker-compose.yml
- `api-server/common/` — SecurityConfig, JwtTokenProvider, JwtAuthenticationFilter, GlobalExceptionHandler, ErrorCode, CorrelationIdFilter, RedisConfig, CorsConfig, PageResponse
- `api-server/store/` — Store 엔티티, StoreRepository, StoreService
- `api-server/auth/` — AuthService, TableAuthController, 로그인 DTO
- `api-server/table/` — StoreTable, TableSession 엔티티, Repository, TableSessionService, AdminTableController
- Flyway V1~V10 — 전체 DB 스키마

---

## 분배 원칙

1. 각 담당자는 자기 소유 디렉토리의 파일만 생성/수정
2. 공통 코드(common/, store/, auth/, table/)는 import만 — 수정 필요 시 팀 리드에게 요청
3. 브랜치: `feat/person{N}-{영역}` → PR 기반 머지

---

## Person 1: 주문 도메인 (api-server/order/)

### 소유 패키지
```
api-server/src/main/java/com/tableorder/order/
├── controller/
│   ├── CustomerOrderController.java    — POST /api/customer/orders, GET /api/customer/orders
│   └── AdminOrderController.java       — PATCH /api/admin/orders/{id}/status, DELETE /api/admin/orders/{id}, GET /api/admin/orders/dashboard
├── entity/
│   ├── Order.java
│   └── OrderItem.java
├── repository/
│   ├── OrderRepository.java
│   └── OrderItemRepository.java
├── service/
│   └── OrderService.java
└── dto/
    ├── CreateOrderRequest.java
    ├── CreateOrderResponse.java
    ├── OrderStatusRequest.java
    ├── OrderListResponse.java
    └── DashboardResponse.java
```

### 담당 스토리 & 엔드포인트
| 스토리 | 엔드포인트 | 설명 |
|---|---|---|
| US-04 | POST /api/customer/orders | 주문 생성 |
| US-06 | GET /api/customer/orders | 현재 세션 주문 조회 |
| US-08 | GET /api/admin/orders/dashboard | 대시보드 요약 |
| US-09 | PATCH /api/admin/orders/{id}/status | 주문 상태 변경 |
| US-11 | DELETE /api/admin/orders/{id} | 주문 삭제 |
| US-13 | GET /api/admin/tables/{id}/history | 과거 주문 내역 |

### 참조 (import만, 수정 금지)
- `common.exception.BusinessException`, `ErrorCode`
- `common.security.SecurityContextUtil`
- `table.entity.TableSession`
- `table.service.TableSessionService`
- `sse.service.SseService` (Person 2가 생성)

### 참고 문서
- `business-logic-model.md` Section 2 (주문 처리 흐름)
- `business-rules.md` BR-02 (주문 규칙)

---

## Person 2: 메뉴 + SSE + 파일 스토리지 (api-server/menu/, sse/, storage/)

### 소유 패키지
```
api-server/src/main/java/com/tableorder/menu/
├── controller/
│   ├── CustomerMenuController.java     — GET /api/customer/menus
│   └── AdminMenuController.java        — POST/PUT/DELETE /api/admin/menus, PATCH /api/admin/menus/order
├── entity/
│   ├── Menu.java
│   └── Category.java
├── repository/
│   ├── MenuRepository.java
│   └── CategoryRepository.java
├── service/
│   └── MenuService.java
└── dto/
    ├── MenuResponse.java
    ├── CreateMenuRequest.java
    └── MenuOrderRequest.java

api-server/src/main/java/com/tableorder/sse/
├── controller/
│   └── SseController.java             — GET /api/admin/sse/subscribe
├── service/
│   └── SseService.java
└── dto/
    └── OrderEvent.java

api-server/src/main/java/com/tableorder/storage/
├── service/
│   └── FileStorageService.java
└── config/
    └── S3Config.java
```

### 담당 스토리 & 엔드포인트
| 스토리 | 엔드포인트 | 설명 |
|---|---|---|
| US-02 | GET /api/customer/menus | 카테고리별 메뉴 조회 |
| US-14 | POST /api/admin/menus | 메뉴 등록 (+ S3 이미지) |
| US-14 | PUT /api/admin/menus/{id} | 메뉴 수정 |
| US-14 | DELETE /api/admin/menus/{id} | 메뉴 삭제 |
| US-14 | PATCH /api/admin/menus/order | 메뉴 순서 변경 |
| US-08 | GET /api/admin/sse/subscribe | SSE 구독 |

### 참조 (import만, 수정 금지)
- `common.exception.BusinessException`, `ErrorCode`
- `common.security.SecurityContextUtil`
- `store.entity.Store`

### 참고 문서
- `business-logic-model.md` Section 4 (메뉴 관리), Section 7 (SSE)
- `business-rules.md` BR-04 (메뉴 규칙)

---

## Person 3: customer-web (고객용 React 앱 전체)

### 소유 디렉토리
```
customer-web/                           ← 전체 소유
├── package.json
├── vite.config.ts
├── tsconfig.json
├── tailwind.config.ts
├── components.json
├── index.html
└── src/
    ├── App.tsx
    ├── main.tsx
    ├── modules/
    │   ├── auth/                       — 자동 로그인, 토큰 관리
    │   ├── welcome/                    — 초기 접속 안내 화면
    │   ├── menu/                       — 카테고리별 메뉴 조회
    │   ├── cart/                       — 장바구니 (localStorage)
    │   ├── order/                      — 주문 생성/조회
    │   └── split-bill/                 — 분할 계산 (클라이언트 전용)
    └── common/
        ├── components/ui/              — shadcn/ui
        ├── hooks/
        ├── lib/api-client.ts
        └── types/
```

### 담당 스토리
| 스토리 | 화면 | 설명 |
|---|---|---|
| US-01 | 자동 로그인 | 로컬 저장소 기반 자동 인증 |
| US-01-1 | 환영 화면 | 매장명, 사용법 안내, "주문 시작하기" |
| US-02 | 메뉴 화면 | 카테고리 탭, 메뉴 카드 |
| US-03 | 장바구니 | 수량 조절, 총액, localStorage |
| US-04 | 주문 확인 | 주문 확정, 성공/실패 처리 |
| US-05 | 분할 계산 | 메뉴별/인원별 분할 (클라이언트) |
| US-06 | 주문 내역 | 현재 세션 주문 목록 |

### API 연동 (api-server 엔드포인트)
- `POST /api/table/auth/login`
- `GET /api/customer/menus`
- `POST /api/customer/orders`
- `GET /api/customer/orders`

### 기술 스택
- React 18 + TypeScript + Vite
- shadcn/ui + Tailwind CSS
- 태블릿 최적화 (터치 44x44px)

---

## Person 4: admin-web + 관리자 API (admin-web/, api-server/admin/)

### 소유 디렉토리 (admin-web)
```
admin-web/                              ← 전체 소유
├── package.json
├── vite.config.ts
├── tsconfig.json
├── tailwind.config.ts
├── components.json
├── index.html
└── src/
    ├── App.tsx
    ├── main.tsx
    ├── modules/
    │   ├── auth/                       — 관리자 로그인
    │   ├── dashboard/                  — 실시간 주문 모니터링 (SSE)
    │   ├── order-management/           — 주문 상태 변경, 삭제
    │   ├── table-management/           — 테이블 설정, 이용 완료, 과거 내역
    │   ├── menu-management/            — 메뉴 CRUD, 이미지 업로드
    │   └── admin-management/           — 계정 관리, 감사 이력 (슈퍼 관리자)
    └── common/
        ├── components/ui/              — shadcn/ui
        ├── hooks/
        ├── lib/api-client.ts, sse-client.ts
        └── types/
```

### 소유 패키지 (api-server)
```
api-server/src/main/java/com/tableorder/admin/
├── controller/
│   └── SuperAdminController.java       — CRUD /api/super-admin/admins, GET /api/super-admin/audit-logs
├── entity/
│   ├── Admin.java
│   ├── SuperAdmin.java
│   └── AdminAuditLog.java
├── repository/
│   ├── AdminRepository.java
│   ├── SuperAdminRepository.java
│   └── AdminAuditLogRepository.java
├── service/
│   └── AdminService.java
└── dto/
    ├── CreateAdminRequest.java
    ├── UpdateAdminRequest.java
    ├── AdminListResponse.java
    └── AuditLogResponse.java
```

### 담당 스토리
| 스토리 | 영역 | 설명 |
|---|---|---|
| US-15 | API + UI | 매장 관리자 계정 CRUD |
| US-16 | API + UI | 감사 이력 조회 |
| US-07 | UI | 관리자 로그인 화면 |
| US-08 | UI | 실시간 대시보드 (SSE 연동) |
| US-09 | UI | 주문 상태 변경 화면 |
| US-10 | UI | 테이블 초기 설정 화면 |
| US-11 | UI | 주문 삭제 화면 |
| US-12 | UI | 이용 완료 화면 |
| US-13 | UI | 과거 주문 내역 화면 |
| US-14 | UI | 메뉴 관리 화면 |

### API 연동 (api-server 엔드포인트)
- `POST /api/admin/auth/login`, `POST /api/super-admin/auth/login`
- `GET /api/admin/orders/dashboard`, `PATCH /api/admin/orders/{id}/status`, `DELETE /api/admin/orders/{id}`
- `POST /api/admin/tables`, `POST /api/admin/tables/{id}/end-session`, `GET /api/admin/tables/{id}/history`
- `POST/PUT/DELETE /api/admin/menus`, `PATCH /api/admin/menus/order`
- `GET /api/admin/sse/subscribe`
- `POST/DELETE /api/super-admin/admins`, `GET /api/super-admin/stores/{id}/admins`, `GET /api/super-admin/audit-logs`

### 기술 스택
- React 18 + TypeScript + Vite
- shadcn/ui + Tailwind CSS
- 데스크톱 최적화

---

## 파일 소유권 매트릭스 (최종)

| 디렉토리 | Person 1 | Person 2 | Person 3 | Person 4 |
|---|---|---|---|---|
| api-server/common/ | 📖 | 📖 | - | 📖 |
| api-server/auth/ | 📖 | - | - | 📖 |
| api-server/store/ | 📖 | 📖 | - | 📖 |
| api-server/table/ | 📖 | 📖 | - | - |
| api-server/order/ | ✏️ 소유 | - | - | - |
| api-server/menu/ | - | ✏️ 소유 | - | - |
| api-server/sse/ | - | ✏️ 소유 | - | - |
| api-server/storage/ | - | ✏️ 소유 | - | - |
| api-server/admin/ | - | - | - | ✏️ 소유 |
| customer-web/ | - | - | ✏️ 소유 | - |
| admin-web/ | - | - | - | ✏️ 소유 |

✏️ = 생성/수정 가능, 📖 = import만 (수정 금지)

---

## 의존성 관계

```
Person 2 (sse/) ←── Person 1 (order/) : SSE 이벤트 발행 호출
                ←── Person 4 (admin-web) : SSE 구독 연동

공통 인프라 (완성) ←── Person 1, 2, 4 : import
API 스펙 문서 ←── Person 3, 4 : 프론트엔드 개발 참조
```

### 협업 포인트
1. Person 1 ↔ Person 2: OrderService가 SseService.publishOrderEvent()를 호출 → Person 2가 SseService 인터페이스를 먼저 정의
2. Person 3, 4: API 스펙은 `business-logic-model.md`의 Request/Response JSON 참조
3. 관리자 로그인 API는 공통(auth/)에 이미 구현 → Person 4는 AdminAuthController 추가 필요 시 auth/ 패키지에 PR
