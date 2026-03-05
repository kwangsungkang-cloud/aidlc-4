# 테이블오더 서비스 — 컴포넌트 의존성

---

## 의존성 매트릭스

| Service | Auth | Store | Table | Menu | Order | Admin | SSE | FileStorage |
|---|---|---|---|---|---|---|---|---|
| AuthService | ● | ● | ● | - | - | ● | - | - |
| TableSessionService | - | - | ● | - | ● | - | - | - |
| OrderService | - | - | ● | - | ● | - | ● | - |
| MenuService | - | - | - | ● | - | - | - | ● |
| AdminService | - | - | - | - | - | ● | - | - |
| SSEService | - | - | - | - | - | - | ● | - |

● = 의존

---

## 통신 패턴

### 프론트엔드 → 백엔드
- **프로토콜**: REST API (HTTPS)
- **인증**: JWT Bearer Token (Authorization 헤더)
- **데이터 형식**: JSON
- **실시간**: SSE (Server-Sent Events) — 관리자 대시보드 전용

### 백엔드 → 데이터베이스
- **프로토콜**: JDBC (MySQL Connector)
- **ORM**: Spring Data JPA / Hibernate
- **연결 풀**: HikariCP

### 백엔드 → S3
- **프로토콜**: AWS SDK (HTTPS)
- **용도**: 메뉴 이미지 업로드/삭제

---

## 데이터 흐름

### 고객 주문 흐름
```
customer-web → [POST /api/customer/orders] → OrderService
  → OrderComponent.createOrder (DB 저장)
  → SSEComponent.publishOrderEvent (실시간 알림)
  → admin-web (SSE 수신, 대시보드 업데이트)
```

### 관리자 주문 상태 변경 흐름
```
admin-web → [PATCH /api/admin/orders/{id}/status] → OrderService
  → OrderComponent.updateOrderStatus (DB 업데이트)
  → SSEComponent.publishOrderEvent (실시간 알림)
  → customer-web (주문 내역 조회 시 반영)
```

### 테이블 이용 완료 흐름
```
admin-web → [POST /api/admin/tables/{id}/end-session] → TableSessionService
  → OrderComponent (주문 이력 이동)
  → TableComponent.endSession (세션 종료, 리셋)
  → SSEComponent.publishOrderEvent (대시보드 업데이트)
```

### 메뉴 등록 흐름
```
admin-web → [POST /api/admin/menus] (multipart) → MenuService
  → FileStorageComponent.uploadImage (S3 업로드)
  → MenuComponent.createMenu (DB 저장, imageUrl 포함)
```

### 슈퍼 관리자 계정 생성 흐름
```
admin-web → [POST /api/super-admin/admins] → AdminService
  → AdminComponent.createAdmin (DB 저장)
  → AdminComponent (AuditLog 자동 기록)
```

---

## 프로젝트 구조 (모노레포)

```
table-order/
├── api-server/          # Spring Boot 백엔드
│   └── src/main/java/
│       └── com/tableorder/
│           ├── auth/
│           ├── store/
│           ├── table/
│           ├── menu/
│           ├── order/
│           ├── admin/
│           ├── sse/
│           ├── storage/
│           └── common/
├── customer-web/        # React 고객용 앱
│   └── src/
│       ├── modules/
│       │   ├── auth/
│       │   ├── welcome/
│       │   ├── menu/
│       │   ├── cart/
│       │   ├── order/
│       │   └── split-bill/
│       └── common/
├── admin-web/           # React 관리자용 앱
│   └── src/
│       ├── modules/
│       │   ├── auth/
│       │   ├── dashboard/
│       │   ├── order-management/
│       │   ├── table-management/
│       │   ├── menu-management/
│       │   └── admin-management/
│       └── common/
└── docs/                # 프로젝트 문서
```
