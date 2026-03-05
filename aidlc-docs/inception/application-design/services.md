# 테이블오더 서비스 — 서비스 레이어 설계

---

## 서비스 아키텍처 개요

```
+------------------+     +------------------+
| customer-web     |     | admin-web        |
| (React/TS)       |     | (React/TS)       |
+--------+---------+     +--------+---------+
         |                         |
         +----------+--------------+
                    |
            +-------v--------+
            | api-server     |
            | (Spring Boot)  |
            +-------+--------+
                    |
         +----------+-----------+
         |                      |
   +-----v------+        +-----v------+
   | MySQL       |        | AWS S3     |
   | (Database)  |        | (Images)   |
   +-------------+        +------------+
```

---

## 1. AuthService
- **책임**: 인증/인가 오케스트레이션
- **오케스트레이션**:
  - 테이블 로그인: StoreComponent → TableComponent → JWT 발급
  - 관리자 로그인: StoreComponent → AdminComponent → 시도 제한 확인 → JWT 발급
  - 슈퍼 관리자 로그인: AdminComponent → JWT 발급
- **의존**: StoreComponent, TableComponent, AdminComponent

## 2. TableSessionService
- **책임**: 테이블 세션 라이프사이클 관리
- **오케스트레이션**:
  - 세션 시작: TableComponent.startSession
  - 이용 완료: OrderComponent (주문 이력 이동) → TableComponent.endSession
- **의존**: TableComponent, OrderComponent

## 3. OrderService
- **책임**: 주문 처리 오케스트레이션
- **오케스트레이션**:
  - 주문 생성: OrderComponent.createOrder → SSEComponent.publishOrderEvent
  - 상태 변경: OrderComponent.updateOrderStatus → SSEComponent.publishOrderEvent
  - 주문 삭제: OrderComponent.deleteOrder → SSEComponent.publishOrderEvent
- **의존**: OrderComponent, SSEComponent, TableComponent

## 4. MenuService
- **책임**: 메뉴 관리 오케스트레이션
- **오케스트레이션**:
  - 메뉴 등록: FileStorageComponent.uploadImage → MenuComponent.createMenu
  - 메뉴 수정: (이미지 변경 시) FileStorageComponent → MenuComponent.updateMenu
  - 메뉴 삭제: MenuComponent.deleteMenu → FileStorageComponent.deleteImage
- **의존**: MenuComponent, FileStorageComponent

## 5. AdminService
- **책임**: 관리자 계정 관리 오케스트레이션
- **오케스트레이션**:
  - 계정 생성: AdminComponent.createAdmin → AuditLog 기록
  - 계정 삭제: AdminComponent.deleteAdmin → AuditLog 기록
  - 이력 조회: AdminComponent.getAuditLogs
- **의존**: AdminComponent

## 6. SSEService
- **책임**: 실시간 이벤트 스트리밍 관리
- **오케스트레이션**:
  - 연결 관리: 매장별 SSE 연결 풀 관리
  - 이벤트 발행: 주문 생성/상태변경/삭제 이벤트 브로드캐스트
- **의존**: SSEComponent

---

## API 엔드포인트 그룹

| 그룹 | Base Path | 인증 | 설명 |
|---|---|---|---|
| Table Auth | /api/table/auth | None → Table Token | 테이블 로그인 |
| Customer Menu | /api/customer/menus | Table Token | 메뉴 조회 |
| Customer Order | /api/customer/orders | Table Token | 주문 생성/조회 |
| Admin Auth | /api/admin/auth | None → Admin JWT | 관리자 로그인 |
| Admin Orders | /api/admin/orders | Admin JWT | 주문 관리 |
| Admin Tables | /api/admin/tables | Admin JWT | 테이블 관리 |
| Admin Menus | /api/admin/menus | Admin JWT | 메뉴 관리 |
| Admin SSE | /api/admin/sse | Admin JWT | 실시간 이벤트 |
| Super Admin | /api/super-admin | SuperAdmin JWT | 계정/이력 관리 |
