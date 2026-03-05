# 테이블오더 서비스 — 컴포넌트 메서드 정의

> 상세 비즈니스 규칙은 Functional Design(CONSTRUCTION) 단계에서 정의

---

## 1. AuthComponent

| 메서드 | 입력 | 출력 | 목적 |
|---|---|---|---|
| loginTable | storeCode, tableNumber, password | TableSession + token | 테이블 태블릿 로그인 |
| loginAdmin | storeCode, username, password | AdminSession + JWT | 관리자 로그인 |
| loginSuperAdmin | username, password | SuperAdminSession + JWT | 슈퍼 관리자 로그인 |
| validateToken | JWT token | TokenPayload | 토큰 검증 |
| refreshToken | JWT token | new JWT token | 토큰 갱신 |
| checkLoginAttempts | storeCode, username | boolean (allowed) | 로그인 시도 제한 확인 |

---

## 2. StoreComponent

| 메서드 | 입력 | 출력 | 목적 |
|---|---|---|---|
| getStore | storeId | Store | 매장 정보 조회 |
| getStoreByCode | storeCode | Store | 매장 코드로 조회 |

---

## 3. TableComponent

| 메서드 | 입력 | 출력 | 목적 |
|---|---|---|---|
| createTable | storeId, tableNumber, password | Table | 테이블 등록 |
| getTablesByStore | storeId | List of Table | 매장 테이블 목록 |
| getActiveSession | tableId | TableSession | 현재 활성 세션 조회 |
| startSession | tableId | TableSession | 새 세션 시작 |
| endSession | sessionId | void | 이용 완료 (주문 이력 이동, 리셋) |

---

## 4. MenuComponent

| 메서드 | 입력 | 출력 | 목적 |
|---|---|---|---|
| getCategories | storeId | List of Category | 카테고리 목록 조회 |
| getMenusByCategory | storeId, categoryId | List of Menu | 카테고리별 메뉴 조회 |
| getMenusByStore | storeId | List of Menu | 매장 전체 메뉴 조회 |
| createMenu | storeId, MenuCreateRequest | Menu | 메뉴 등록 |
| updateMenu | menuId, MenuUpdateRequest | Menu | 메뉴 수정 |
| deleteMenu | menuId | void | 메뉴 삭제 |
| updateMenuOrder | storeId, List of menuId+order | void | 노출 순서 변경 |
| uploadMenuImage | menuId, imageFile | imageUrl | 이미지 S3 업로드 |

---

## 5. OrderComponent

| 메서드 | 입력 | 출력 | 목적 |
|---|---|---|---|
| createOrder | sessionId, List of OrderItem | Order | 주문 생성 |
| getOrdersBySession | sessionId | List of Order | 현재 세션 주문 조회 |
| getOrderDetail | orderId | Order + items | 주문 상세 조회 |
| updateOrderStatus | orderId, newStatus | Order | 주문 상태 변경 |
| deleteOrder | orderId | void | 주문 삭제 |
| getOrderHistory | tableId, dateRange | List of Order | 과거 주문 이력 조회 |
| getTableSummary | storeId | List of TableOrderSummary | 테이블별 주문 요약 (대시보드) |

---

## 6. AdminComponent

| 메서드 | 입력 | 출력 | 목적 |
|---|---|---|---|
| createAdmin | storeId, AdminCreateRequest | Admin | 매장 관리자 생성 |
| updateAdmin | adminId, AdminUpdateRequest | Admin | 매장 관리자 수정 |
| deleteAdmin | adminId | void | 매장 관리자 삭제 |
| getAdminsByStore | storeId | List of Admin | 매장별 관리자 조회 |
| getAllAdmins | - | List of Admin | 전체 관리자 조회 |
| getAuditLogs | dateRange, actionType | List of AuditLog | 활동 이력 조회 |

---

## 7. SSEComponent

| 메서드 | 입력 | 출력 | 목적 |
|---|---|---|---|
| subscribe | storeId | SseEmitter | SSE 연결 생성 |
| publishOrderEvent | storeId, OrderEvent | void | 주문 이벤트 발행 |
| removeConnection | storeId, emitterId | void | 연결 제거 |

---

## 8. FileStorageComponent

| 메서드 | 입력 | 출력 | 목적 |
|---|---|---|---|
| uploadImage | imageFile, path | imageUrl | S3 이미지 업로드 |
| deleteImage | imageUrl | void | S3 이미지 삭제 |
