# Business Logic Model — api-server (상세)

---

## 1. 인증 흐름

### 1.1 테이블 태블릿 로그인

**엔드포인트**: `POST /api/table/auth/login`

**Request Body**:
```json
{
  "storeCode": "string (required, max 50)",
  "tableNumber": "integer (required)",
  "password": "string (required)"
}
```

**처리 로직**:
1. 입력 검증
   - storeCode: null/빈값 체크, max 50자
   - tableNumber: null 체크, 양수 검증
   - password: null/빈값 체크
   - 검증 실패 시 → `400 Bad Request` + 필드별 에러 메시지

2. 매장 조회
   - `SELECT * FROM store WHERE store_code = :storeCode`
   - 결과 없음 → `404 Not Found` ("매장을 찾을 수 없습니다")

3. 테이블 조회
   - `SELECT * FROM store_table WHERE store_id = :storeId AND table_number = :tableNumber`
   - 결과 없음 → `404 Not Found` ("테이블을 찾을 수 없습니다")

4. 비밀번호 검증
   - `BCrypt.matches(password, table.password)`
   - 불일치 → `401 Unauthorized` ("비밀번호가 일치하지 않습니다")

5. 활성 세션 확인
   - `SELECT * FROM table_session WHERE table_id = :tableId AND status = 'ACTIVE'`
   - **Case A**: 활성 세션 존재 + `expires_at > now()`
     - 기존 세션 재사용
     - 기존 세션의 sessionToken으로 JWT 재발급
   - **Case B**: 활성 세션 존재 + `expires_at <= now()`
     - 기존 세션 상태 변경: `status = 'COMPLETED'`, `completed_at = now()`
     - 새 세션 생성 (Step 6으로)
   - **Case C**: 활성 세션 없음
     - 새 세션 생성 (Step 6으로)

6. 새 세션 생성 (Case B, C)
   - `sessionToken = UUID.randomUUID()`
   - `status = 'ACTIVE'`
   - `started_at = now()`
   - `expires_at = now() + 16 hours`
   - `INSERT INTO table_session (...)`

7. JWT 토큰 발급
   - 페이로드:
     ```json
     {
       "sub": "table-session",
       "sessionId": 123,
       "tableId": 45,
       "storeId": 1,
       "tableNumber": 5,
       "iat": 1709625600,
       "exp": 1709683200
     }
     ```
   - 서명: HMAC-SHA256 (서버 시크릿 키)
   - 만료: 16시간

**Response (200 OK)**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "storeName": "맛있는 식당",
  "storeCode": "STORE001",
  "tableNumber": 5,
  "sessionId": 123,
  "isNewSession": true
}
```

**에러 응답**:
| HTTP Status | 조건 | 메시지 |
|---|---|---|
| 400 | 입력 검증 실패 | 필드별 에러 메시지 |
| 401 | 비밀번호 불일치 | "비밀번호가 일치하지 않습니다" |
| 404 | 매장/테이블 없음 | "매장/테이블을 찾을 수 없습니다" |

---

### 1.2 매장 관리자 로그인

**엔드포인트**: `POST /api/admin/auth/login`

**Request Body**:
```json
{
  "storeCode": "string (required, max 50)",
  "username": "string (required, max 50)",
  "password": "string (required)"
}
```

**처리 로직**:
1. 입력 검증
   - storeCode, username, password: null/빈값 체크
   - 검증 실패 시 → `400 Bad Request`

2. 매장 조회
   - `SELECT * FROM store WHERE store_code = :storeCode`
   - 결과 없음 → `401 Unauthorized` ("인증 정보가 올바르지 않습니다")
   - 보안: 매장 존재 여부를 노출하지 않기 위해 401 사용

3. 관리자 조회
   - `SELECT * FROM admin WHERE store_id = :storeId AND username = :username`
   - 결과 없음 → `401 Unauthorized` ("인증 정보가 올바르지 않습니다")

4. 잠금 상태 확인
   - `admin.lockedUntil != null AND admin.lockedUntil > now()`
   - 잠금 상태 → `423 Locked` ("계정이 잠겨 있습니다. {남은시간}분 후 다시 시도해주세요")

5. 비밀번호 검증
   - `BCrypt.matches(password, admin.password)`
   - **성공 시**:
     - `admin.loginAttempts = 0`
     - `admin.lockedUntil = null`
     - `UPDATE admin SET login_attempts = 0, locked_until = null WHERE id = :adminId`
   - **실패 시**:
     - `admin.loginAttempts += 1`
     - 5회 이상 실패 시: `admin.lockedUntil = now() + 30분`
     - `UPDATE admin SET login_attempts = :attempts, locked_until = :lockedUntil WHERE id = :adminId`
     - → `401 Unauthorized` ("인증 정보가 올바르지 않습니다. 남은 시도: {5 - attempts}회")

6. JWT 토큰 발급
   - 페이로드:
     ```json
     {
       "sub": "admin",
       "adminId": 10,
       "storeId": 1,
       "role": "STORE_ADMIN",
       "iat": 1709625600,
       "exp": 1709683200
     }
     ```
   - 만료: 16시간

**Response (200 OK)**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "storeName": "맛있는 식당",
  "storeCode": "STORE001",
  "adminId": 10,
  "username": "admin1"
}
```

**에러 응답**:
| HTTP Status | 조건 | 메시지 |
|---|---|---|
| 400 | 입력 검증 실패 | 필드별 에러 메시지 |
| 401 | 인증 실패 | "인증 정보가 올바르지 않습니다" |
| 423 | 계정 잠금 | "계정이 잠겨 있습니다" |

---

### 1.3 슈퍼 관리자 로그인

**엔드포인트**: `POST /api/super-admin/auth/login`

**Request Body**:
```json
{
  "username": "string (required)",
  "password": "string (required)"
}
```

**처리 로직**:
1. 입력 검증 → 실패 시 `400`
2. SuperAdmin 조회 (`username`) → 없으면 `401`
3. 비밀번호 검증 (bcrypt) → 실패 시 `401`
4. JWT 발급 (role=SUPER_ADMIN, 16시간 만료)

**Response (200 OK)**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "superAdminId": 1,
  "username": "superadmin"
}
```

---

### 1.4 토큰 검증 (공통 미들웨어)

**적용**: 모든 인증 필요 엔드포인트의 `Authorization: Bearer {token}` 헤더

**처리 로직**:
1. Authorization 헤더 존재 확인 → 없으면 `401`
2. "Bearer " 접두사 제거 후 토큰 추출
3. JWT 서명 검증 (HMAC-SHA256) → 실패 시 `401`
4. 만료 시간 확인 (`exp < now()`) → 만료 시 `401` ("토큰이 만료되었습니다")
5. 페이로드에서 사용자 정보 추출 → SecurityContext에 저장
6. role 기반 엔드포인트 접근 제어:
   - `/api/customer/**` → `sub = "table-session"` 필요
   - `/api/admin/**` → `role = "STORE_ADMIN"` 필요
   - `/api/super-admin/**` → `role = "SUPER_ADMIN"` 필요
   - 권한 불일치 → `403 Forbidden`


---

## 2. 주문 처리 흐름

### 2.1 주문 생성

**엔드포인트**: `POST /api/customer/orders`
**인증**: Table Token (sessionId, tableId, storeId)

**Request Body**:
```json
{
  "items": [
    { "menuId": 1, "quantity": 2 },
    { "menuId": 3, "quantity": 1 }
  ]
}
```

**처리 로직**:
1. 입력 검증
   - items: null/빈 배열 체크 → `400` ("주문 항목이 비어있습니다")
   - 각 item: menuId null 체크, quantity >= 1 체크
   - 검증 실패 시 → `400 Bad Request`

2. 세션 검증
   - 토큰에서 sessionId 추출
   - `SELECT * FROM table_session WHERE id = :sessionId`
   - 세션 없음 → `403 Forbidden`
   - `status != 'ACTIVE'` → `403` ("세션이 종료되었습니다")
   - `expires_at <= now()` → 세션 자동 COMPLETED 처리 → `403` ("세션이 만료되었습니다")

3. 메뉴 검증 (각 item에 대해)
   - `SELECT * FROM menu WHERE id = :menuId`
   - 메뉴 없음 → `400` ("메뉴를 찾을 수 없습니다: menuId={id}")
   - `menu.storeId != token.storeId` → `400` ("해당 매장의 메뉴가 아닙니다")

4. 주문 번호 생성
   - 형식: `ORD-{YYYYMMDD}-{4자리 시퀀스}`
   - 예: `ORD-20260305-0001`
   - 시퀀스: 해당 날짜의 해당 매장 주문 수 + 1
   - `SELECT COUNT(*) FROM orders o JOIN table_session ts ON o.session_id = ts.id JOIN store_table st ON ts.table_id = st.id WHERE st.store_id = :storeId AND DATE(o.created_at) = CURDATE()`

5. OrderItem 생성 (각 item에 대해)
   - `menuName = menu.name` (스냅샷)
   - `unitPrice = menu.price` (스냅샷)
   - `subtotal = quantity * unitPrice`

6. totalAmount 계산
   - `totalAmount = SUM(item.subtotal for all items)`

7. Order 저장
   - `status = 'PENDING'`
   - `created_at = now()`
   - `updated_at = now()`
   - 트랜잭션: Order INSERT → OrderItem 일괄 INSERT (원자적)

8. SSE 이벤트 발행
   - 대상: `storeId`의 모든 SSE 연결
   - 이벤트 타입: `NEW_ORDER`
   - 데이터:
     ```json
     {
       "eventType": "NEW_ORDER",
       "orderId": 100,
       "orderNumber": "ORD-20260305-0001",
       "tableNumber": 5,
       "totalAmount": 35000,
       "itemCount": 3,
       "itemSummary": "치킨 x2, 맥주 x1",
       "createdAt": "2026-03-05T12:30:00"
     }
     ```

**Response (201 Created)**:
```json
{
  "orderId": 100,
  "orderNumber": "ORD-20260305-0001",
  "status": "PENDING",
  "totalAmount": 35000,
  "items": [
    {
      "orderItemId": 1,
      "menuId": 1,
      "menuName": "치킨",
      "quantity": 2,
      "unitPrice": 15000,
      "subtotal": 30000
    },
    {
      "orderItemId": 2,
      "menuId": 3,
      "menuName": "맥주",
      "quantity": 1,
      "unitPrice": 5000,
      "subtotal": 5000
    }
  ],
  "createdAt": "2026-03-05T12:30:00"
}
```

**에러 응답**:
| HTTP Status | 조건 | 메시지 |
|---|---|---|
| 400 | 입력 검증 실패 / 메뉴 없음 | 상세 에러 메시지 |
| 403 | 세션 비활성/만료 | "세션이 종료/만료되었습니다" |

---

### 2.2 주문 상태 변경

**엔드포인트**: `PATCH /api/admin/orders/{orderId}/status`
**인증**: Admin JWT (storeId 검증)

**Request Body**:
```json
{
  "status": "PREPARING"
}
```

**처리 로직**:
1. 입력 검증
   - status: `PREPARING` 또는 `COMPLETED`만 허용
   - 그 외 값 → `400` ("유효하지 않은 상태입니다")

2. 주문 조회
   - `SELECT o.*, ts.table_id, st.store_id, st.table_number FROM orders o JOIN table_session ts ON o.session_id = ts.id JOIN store_table st ON ts.table_id = st.id WHERE o.id = :orderId`
   - 없음 → `404` ("주문을 찾을 수 없습니다")

3. 매장 소속 검증
   - `order.storeId != token.storeId` → `403` ("접근 권한이 없습니다")

4. 상태 전이 검증
   - 허용되는 전이:
     - `PENDING` → `PREPARING` ✅
     - `PREPARING` → `COMPLETED` ✅
   - 허용되지 않는 전이:
     - `PENDING` → `COMPLETED` ❌ ("준비중 단계를 거쳐야 합니다")
     - `PREPARING` → `PENDING` ❌ ("이전 상태로 되돌릴 수 없습니다")
     - `COMPLETED` → 어떤 상태든 ❌ ("완료된 주문은 변경할 수 없습니다")
   - 전이 불가 → `400` (상세 메시지)

5. 상태 업데이트
   - `UPDATE orders SET status = :newStatus, updated_at = now() WHERE id = :orderId`

6. SSE 이벤트 발행
   - 이벤트 타입: `ORDER_STATUS_CHANGED`
   - 데이터:
     ```json
     {
       "eventType": "ORDER_STATUS_CHANGED",
       "orderId": 100,
       "orderNumber": "ORD-20260305-0001",
       "tableNumber": 5,
       "previousStatus": "PENDING",
       "newStatus": "PREPARING",
       "updatedAt": "2026-03-05T12:35:00"
     }
     ```

**Response (200 OK)**:
```json
{
  "orderId": 100,
  "orderNumber": "ORD-20260305-0001",
  "previousStatus": "PENDING",
  "status": "PREPARING",
  "updatedAt": "2026-03-05T12:35:00"
}
```

---

### 2.3 주문 삭제

**엔드포인트**: `DELETE /api/admin/orders/{orderId}`
**인증**: Admin JWT (storeId 검증)

**처리 로직**:
1. 주문 조회 (2.2와 동일한 JOIN 쿼리)
   - 없음 → `404`

2. 매장 소속 검증
   - `order.storeId != token.storeId` → `403`

3. 주문 삭제 (트랜잭션)
   - `DELETE FROM order_item WHERE order_id = :orderId`
   - `DELETE FROM orders WHERE id = :orderId`

4. SSE 이벤트 발행
   - 이벤트 타입: `ORDER_DELETED`
   - 데이터:
     ```json
     {
       "eventType": "ORDER_DELETED",
       "orderId": 100,
       "orderNumber": "ORD-20260305-0001",
       "tableNumber": 5,
       "deletedAmount": 35000,
       "deletedAt": "2026-03-05T12:40:00"
     }
     ```

**Response (200 OK)**:
```json
{
  "success": true,
  "deletedOrderId": 100,
  "deletedOrderNumber": "ORD-20260305-0001"
}
```

---

### 2.4 현재 세션 주문 조회 (고객)

**엔드포인트**: `GET /api/customer/orders`
**인증**: Table Token (sessionId)

**처리 로직**:
1. 토큰에서 sessionId 추출
2. 세션 검증 (ACTIVE 상태 확인)
3. 주문 조회
   - `SELECT o.*, oi.* FROM orders o LEFT JOIN order_item oi ON o.id = oi.order_id WHERE o.session_id = :sessionId ORDER BY o.created_at ASC`
4. 주문별 항목 그룹화

**Response (200 OK)**:
```json
{
  "sessionId": 123,
  "orders": [
    {
      "orderId": 100,
      "orderNumber": "ORD-20260305-0001",
      "status": "PREPARING",
      "totalAmount": 35000,
      "items": [
        { "menuName": "치킨", "quantity": 2, "unitPrice": 15000, "subtotal": 30000 },
        { "menuName": "맥주", "quantity": 1, "unitPrice": 5000, "subtotal": 5000 }
      ],
      "createdAt": "2026-03-05T12:30:00"
    }
  ],
  "sessionTotalAmount": 35000
}
```

---

### 2.5 테이블별 주문 요약 (관리자 대시보드)

**엔드포인트**: `GET /api/admin/orders/dashboard`
**인증**: Admin JWT (storeId)

**처리 로직**:
1. 토큰에서 storeId 추출
2. 해당 매장의 모든 테이블 + 활성 세션 + 주문 조회
   ```sql
   SELECT st.id as table_id, st.table_number,
          ts.id as session_id, ts.status as session_status,
          o.id as order_id, o.order_number, o.status as order_status,
          o.total_amount, o.created_at
   FROM store_table st
   LEFT JOIN table_session ts ON st.id = ts.table_id AND ts.status = 'ACTIVE'
   LEFT JOIN orders o ON ts.id = o.session_id
   WHERE st.store_id = :storeId
   ORDER BY st.table_number ASC, o.created_at DESC
   ```
3. 테이블별 그룹화
4. 각 테이블: 총 주문액 계산, 최신 주문 N개 추출

**Response (200 OK)**:
```json
{
  "storeId": 1,
  "tables": [
    {
      "tableId": 45,
      "tableNumber": 5,
      "sessionId": 123,
      "sessionStatus": "ACTIVE",
      "totalOrderAmount": 35000,
      "orderCount": 2,
      "recentOrders": [
        {
          "orderId": 101,
          "orderNumber": "ORD-20260305-0002",
          "status": "PENDING",
          "totalAmount": 20000,
          "itemSummary": "피자 x1, 콜라 x2",
          "createdAt": "2026-03-05T12:45:00"
        }
      ]
    },
    {
      "tableId": 46,
      "tableNumber": 6,
      "sessionId": null,
      "sessionStatus": null,
      "totalOrderAmount": 0,
      "orderCount": 0,
      "recentOrders": []
    }
  ]
}
```


---

## 3. 테이블 세션 라이프사이클

### 3.1 테이블 등록 (초기 설정)

**엔드포인트**: `POST /api/admin/tables`
**인증**: Admin JWT (storeId)

**Request Body**:
```json
{
  "tableNumber": 5,
  "password": "table1234"
}
```

**처리 로직**:
1. 입력 검증
   - tableNumber: null 체크, 양수 검증
   - password: null/빈값 체크, 최소 4자
   - 검증 실패 → `400`

2. 중복 확인
   - `SELECT * FROM store_table WHERE store_id = :storeId AND table_number = :tableNumber`
   - 이미 존재 → `409 Conflict` ("이미 등록된 테이블 번호입니다")

3. 비밀번호 해싱
   - `BCrypt.encode(password)` (strength=10)

4. 테이블 저장
   - `INSERT INTO store_table (store_id, table_number, password, created_at)`

**Response (201 Created)**:
```json
{
  "tableId": 45,
  "storeId": 1,
  "tableNumber": 5,
  "createdAt": "2026-03-05T09:00:00"
}
```

---

### 3.2 이용 완료 (세션 종료)

**엔드포인트**: `POST /api/admin/tables/{tableId}/end-session`
**인증**: Admin JWT (storeId)

**처리 로직**:
1. 테이블 조회
   - `SELECT * FROM store_table WHERE id = :tableId`
   - 없음 → `404`
   - `table.storeId != token.storeId` → `403`

2. 활성 세션 조회
   - `SELECT * FROM table_session WHERE table_id = :tableId AND status = 'ACTIVE'`
   - 없음 → `400` ("활성 세션이 없습니다")

3. 미완료 주문 일괄 완료 처리 (트랜잭션)
   - `UPDATE orders SET status = 'COMPLETED', updated_at = now() WHERE session_id = :sessionId AND status != 'COMPLETED'`

4. 세션 종료
   - `UPDATE table_session SET status = 'COMPLETED', completed_at = now() WHERE id = :sessionId`

5. SSE 이벤트 발행
   - 이벤트 타입: `SESSION_COMPLETED`
   - 데이터:
     ```json
     {
       "eventType": "SESSION_COMPLETED",
       "tableId": 45,
       "tableNumber": 5,
       "sessionId": 123,
       "completedAt": "2026-03-05T14:00:00",
       "totalOrderAmount": 85000,
       "orderCount": 3
     }
     ```

**Response (200 OK)**:
```json
{
  "sessionId": 123,
  "tableNumber": 5,
  "status": "COMPLETED",
  "completedAt": "2026-03-05T14:00:00",
  "totalOrderAmount": 85000,
  "orderCount": 3
}
```

---

### 3.3 과거 주문 내역 조회

**엔드포인트**: `GET /api/admin/tables/{tableId}/history?startDate=2026-03-01&endDate=2026-03-05`
**인증**: Admin JWT (storeId)

**Query Parameters**:
| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| startDate | LocalDate | 선택 | 조회 시작일 (기본: 7일 전) |
| endDate | LocalDate | 선택 | 조회 종료일 (기본: 오늘) |

**처리 로직**:
1. 테이블 조회 + 매장 소속 검증
2. 날짜 범위 기본값 설정 (미입력 시 최근 7일)
3. 완료된 세션 + 주문 조회
   ```sql
   SELECT ts.id as session_id, ts.started_at, ts.completed_at,
          o.id as order_id, o.order_number, o.total_amount, o.created_at,
          oi.menu_name, oi.quantity, oi.unit_price, oi.subtotal
   FROM table_session ts
   JOIN orders o ON ts.id = o.session_id
   JOIN order_item oi ON o.id = oi.order_id
   WHERE ts.table_id = :tableId
     AND ts.status = 'COMPLETED'
     AND ts.completed_at BETWEEN :startDate AND :endDate+1
   ORDER BY ts.completed_at DESC, o.created_at ASC
   ```
4. 세션별 → 주문별 → 항목별 그룹화

**Response (200 OK)**:
```json
{
  "tableId": 45,
  "tableNumber": 5,
  "sessions": [
    {
      "sessionId": 122,
      "startedAt": "2026-03-04T11:00:00",
      "completedAt": "2026-03-04T13:30:00",
      "totalAmount": 65000,
      "orders": [
        {
          "orderId": 95,
          "orderNumber": "ORD-20260304-0003",
          "totalAmount": 40000,
          "items": [
            { "menuName": "스테이크", "quantity": 1, "unitPrice": 30000, "subtotal": 30000 },
            { "menuName": "와인", "quantity": 1, "unitPrice": 10000, "subtotal": 10000 }
          ],
          "createdAt": "2026-03-04T11:15:00"
        }
      ]
    }
  ]
}
```


---

## 4. 메뉴 관리 흐름

### 4.1 카테고리별 메뉴 조회 (고객)

**엔드포인트**: `GET /api/customer/menus`
**인증**: Table Token (storeId)

**처리 로직**:
1. 토큰에서 storeId 추출
2. 카테고리 + 메뉴 조회
   ```sql
   SELECT c.id as category_id, c.name as category_name, c.display_order as category_order,
          m.id as menu_id, m.name as menu_name, m.price, m.description, m.image_url, m.display_order
   FROM category c
   LEFT JOIN menu m ON c.id = m.category_id
   WHERE c.store_id = :storeId
   ORDER BY c.display_order ASC, m.display_order ASC
   ```
3. 카테고리별 그룹화

**Response (200 OK)**:
```json
{
  "categories": [
    {
      "categoryId": 1,
      "name": "메인 메뉴",
      "menus": [
        {
          "menuId": 1,
          "name": "치킨",
          "price": 15000,
          "description": "바삭한 후라이드 치킨",
          "imageUrl": "https://s3.amazonaws.com/bucket/chicken.jpg"
        }
      ]
    }
  ]
}
```

---

### 4.2 메뉴 등록

**엔드포인트**: `POST /api/admin/menus`
**인증**: Admin JWT (storeId)
**Content-Type**: `multipart/form-data`

**Request**:
| 필드 | 타입 | 필수 | 검증 |
|---|---|---|---|
| categoryId | Long | ✅ | 존재 + 같은 매장 소속 |
| name | String | ✅ | max 100자, 빈값 불가 |
| price | Integer | ✅ | 1 이상 10,000,000 이하 |
| description | String | ❌ | max 500자 |
| image | MultipartFile | ❌ | jpg/png/webp, max 5MB |

**처리 로직**:
1. 입력 검증
   - name: null/빈값 체크, max 100자
   - price: null 체크, 1 <= price <= 10,000,000
   - categoryId: null 체크
   - 검증 실패 → `400` (필드별 에러)

2. 카테고리 검증
   - `SELECT * FROM category WHERE id = :categoryId AND store_id = :storeId`
   - 없음 → `400` ("카테고리를 찾을 수 없습니다")

3. 이미지 업로드 (image 존재 시)
   - 파일 형식 검증: `Content-Type in [image/jpeg, image/png, image/webp]`
     - 불일치 → `400` ("지원하지 않는 이미지 형식입니다 (jpg, png, webp만 허용)")
   - 파일 크기 검증: `size <= 5MB`
     - 초과 → `400` ("이미지 크기는 5MB 이하여야 합니다")
   - S3 업로드 경로: `menus/{storeId}/{UUID}.{extension}`
   - S3 업로드 실행 → imageUrl 획득
   - 업로드 실패 → `500` ("이미지 업로드에 실패했습니다")

4. displayOrder 결정
   - `SELECT COALESCE(MAX(display_order), 0) + 1 FROM menu WHERE store_id = :storeId AND category_id = :categoryId`

5. 메뉴 저장
   - `INSERT INTO menu (store_id, category_id, name, price, description, image_url, display_order, created_at, updated_at)`

**Response (201 Created)**:
```json
{
  "menuId": 10,
  "categoryId": 1,
  "name": "새 메뉴",
  "price": 12000,
  "description": "맛있는 새 메뉴",
  "imageUrl": "https://s3.amazonaws.com/bucket/menus/1/abc123.jpg",
  "displayOrder": 5,
  "createdAt": "2026-03-05T10:00:00"
}
```

---

### 4.3 메뉴 수정

**엔드포인트**: `PUT /api/admin/menus/{menuId}`
**인증**: Admin JWT (storeId)
**Content-Type**: `multipart/form-data`

**처리 로직**:
1. 메뉴 조회 + 매장 소속 검증
2. 입력 검증 (4.2와 동일)
3. 이미지 변경 시:
   - 기존 이미지 S3 삭제 (imageUrl 존재 시)
   - 새 이미지 S3 업로드
4. 메뉴 업데이트
   - `UPDATE menu SET name=:name, price=:price, description=:desc, category_id=:catId, image_url=:imgUrl, updated_at=now() WHERE id=:menuId`

---

### 4.4 메뉴 삭제

**엔드포인트**: `DELETE /api/admin/menus/{menuId}`
**인증**: Admin JWT (storeId)

**처리 로직**:
1. 메뉴 조회 + 매장 소속 검증
2. 이미지 S3 삭제 (imageUrl 존재 시)
3. 메뉴 삭제
   - `DELETE FROM menu WHERE id = :menuId`
   - 기존 OrderItem의 menuName, unitPrice 스냅샷은 유지됨 (FK 제약 없음)

**Response (200 OK)**:
```json
{ "success": true, "deletedMenuId": 10 }
```

---

### 4.5 메뉴 노출 순서 변경

**엔드포인트**: `PATCH /api/admin/menus/order`
**인증**: Admin JWT (storeId)

**Request Body**:
```json
{
  "items": [
    { "menuId": 1, "displayOrder": 1 },
    { "menuId": 3, "displayOrder": 2 },
    { "menuId": 2, "displayOrder": 3 }
  ]
}
```

**처리 로직**:
1. 모든 menuId가 해당 매장 소속인지 확인
   - 불일치 → `403`
2. 일괄 업데이트 (트랜잭션)
   - 각 item: `UPDATE menu SET display_order = :order, updated_at = now() WHERE id = :menuId`

**Response (200 OK)**:
```json
{ "success": true, "updatedCount": 3 }
```


---

## 5. 관리자 계정 관리 흐름

### 5.1 매장 관리자 생성

**엔드포인트**: `POST /api/super-admin/admins`
**인증**: SuperAdmin JWT (role=SUPER_ADMIN)

**Request Body**:
```json
{
  "storeId": 1,
  "username": "admin1",
  "password": "securePass123"
}
```

**처리 로직**:
1. 입력 검증
   - storeId: null 체크
   - username: null/빈값 체크, max 50자, 영문+숫자만 허용 (`^[a-zA-Z0-9]+$`)
   - password: null/빈값 체크, 최소 8자
   - 검증 실패 → `400 Bad Request` (필드별 에러)

2. 매장 존재 확인
   - `SELECT * FROM store WHERE id = :storeId`
   - 없음 → `404 Not Found` ("매장을 찾을 수 없습니다")

3. username 중복 확인
   - `SELECT * FROM admin WHERE store_id = :storeId AND username = :username`
   - 이미 존재 → `409 Conflict` ("이미 사용 중인 사용자명입니다")

4. 비밀번호 해싱
   - `BCrypt.encode(password)` (strength=10)

5. 관리자 저장 (트랜잭션)
   - `INSERT INTO admin (store_id, username, password, login_attempts, locked_until, created_at) VALUES (:storeId, :username, :hashedPassword, 0, null, now())`

6. 감사 이력 기록 (같은 트랜잭션)
   - `INSERT INTO admin_audit_log (performed_by, target_admin_id, target_username, store_id, action_type, performed_at) VALUES (:superAdminId, :newAdminId, :username, :storeId, 'CREATED', now())`

**Response (201 Created)**:
```json
{
  "adminId": 10,
  "storeId": 1,
  "username": "admin1",
  "createdAt": "2026-03-05T10:00:00"
}
```

**에러 응답**:
| HTTP Status | 조건 | 메시지 |
|---|---|---|
| 400 | 입력 검증 실패 | 필드별 에러 메시지 |
| 404 | 매장 없음 | "매장을 찾을 수 없습니다" |
| 409 | username 중복 | "이미 사용 중인 사용자명입니다" |

---

### 5.2 매장 관리자 수정

**엔드포인트**: `PUT /api/super-admin/admins/{adminId}`
**인증**: SuperAdmin JWT (role=SUPER_ADMIN)

**Request Body**:
```json
{
  "password": "newSecurePass456"
}
```

**처리 로직**:
1. 입력 검증
   - password: null/빈값 체크, 최소 8자
   - 검증 실패 → `400`

2. 관리자 조회
   - `SELECT * FROM admin WHERE id = :adminId`
   - 없음 → `404 Not Found` ("관리자를 찾을 수 없습니다")

3. 비밀번호 해싱
   - `BCrypt.encode(password)` (strength=10)

4. 관리자 업데이트
   - `UPDATE admin SET password = :hashedPassword, login_attempts = 0, locked_until = null WHERE id = :adminId`
   - 비밀번호 변경 시 로그인 시도 횟수 및 잠금 상태 초기화

**Response (200 OK)**:
```json
{
  "adminId": 10,
  "storeId": 1,
  "username": "admin1",
  "message": "비밀번호가 변경되었습니다"
}
```

**에러 응답**:
| HTTP Status | 조건 | 메시지 |
|---|---|---|
| 400 | 입력 검증 실패 | 필드별 에러 메시지 |
| 404 | 관리자 없음 | "관리자를 찾을 수 없습니다" |

---

### 5.3 매장 관리자 삭제

**엔드포인트**: `DELETE /api/super-admin/admins/{adminId}`
**인증**: SuperAdmin JWT (role=SUPER_ADMIN)

**처리 로직**:
1. 관리자 조회
   - `SELECT * FROM admin WHERE id = :adminId`
   - 없음 → `404 Not Found` ("관리자를 찾을 수 없습니다")

2. 삭제 전 정보 스냅샷
   - `targetUsername = admin.username`
   - `targetStoreId = admin.storeId`

3. 관리자 삭제 + 감사 이력 기록 (트랜잭션)
   - `DELETE FROM admin WHERE id = :adminId`
   - `INSERT INTO admin_audit_log (performed_by, target_admin_id, target_username, store_id, action_type, performed_at) VALUES (:superAdminId, :adminId, :targetUsername, :targetStoreId, 'DELETED', now())`

**Response (200 OK)**:
```json
{
  "success": true,
  "deletedAdminId": 10,
  "deletedUsername": "admin1"
}
```

**에러 응답**:
| HTTP Status | 조건 | 메시지 |
|---|---|---|
| 404 | 관리자 없음 | "관리자를 찾을 수 없습니다" |

---

### 5.4 매장별 관리자 목록 조회

**엔드포인트**: `GET /api/super-admin/stores/{storeId}/admins`
**인증**: SuperAdmin JWT (role=SUPER_ADMIN)

**처리 로직**:
1. 매장 존재 확인
   - `SELECT * FROM store WHERE id = :storeId`
   - 없음 → `404 Not Found` ("매장을 찾을 수 없습니다")

2. 관리자 목록 조회
   - `SELECT id, store_id, username, login_attempts, locked_until, created_at FROM admin WHERE store_id = :storeId ORDER BY created_at ASC`
   - 비밀번호 필드는 절대 반환하지 않음

**Response (200 OK)**:
```json
{
  "storeId": 1,
  "storeName": "맛있는 식당",
  "admins": [
    {
      "adminId": 10,
      "username": "admin1",
      "loginAttempts": 0,
      "isLocked": false,
      "createdAt": "2026-03-01T09:00:00"
    },
    {
      "adminId": 11,
      "username": "admin2",
      "loginAttempts": 3,
      "isLocked": false,
      "createdAt": "2026-03-02T10:00:00"
    }
  ],
  "totalCount": 2
}
```


---

## 6. 슈퍼 관리자 감사 이력 흐름

### 6.1 감사 이력 조회

**엔드포인트**: `GET /api/super-admin/audit-logs?startDate=2026-03-01&endDate=2026-03-05&actionType=CREATED`
**인증**: SuperAdmin JWT (role=SUPER_ADMIN)

**Query Parameters**:
| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| startDate | LocalDate | 선택 | 조회 시작일 (기본: 30일 전) |
| endDate | LocalDate | 선택 | 조회 종료일 (기본: 오늘) |
| actionType | String | 선택 | 액션 유형 필터 (CREATED / DELETED, 미입력 시 전체) |
| page | Integer | 선택 | 페이지 번호 (기본: 0) |
| size | Integer | 선택 | 페이지 크기 (기본: 20, 최대: 100) |

**처리 로직**:
1. 입력 검증
   - startDate, endDate: 유효한 날짜 형식 (yyyy-MM-dd)
   - startDate <= endDate 검증 → 위반 시 `400` ("시작일이 종료일보다 클 수 없습니다")
   - actionType: null 또는 `CREATED` / `DELETED`만 허용 → 그 외 `400` ("유효하지 않은 액션 유형입니다")
   - page: 0 이상 → 음수 시 `400`
   - size: 1 이상 100 이하 → 범위 초과 시 `400`

2. 날짜 범위 기본값 설정
   - startDate 미입력 → `now() - 30 days`
   - endDate 미입력 → `now()`

3. 감사 이력 조회
   ```sql
   SELECT al.id, al.performed_by, sa.username as performer_username,
          al.target_admin_id, al.target_username, al.store_id,
          s.store_name, al.action_type, al.performed_at
   FROM admin_audit_log al
   JOIN super_admin sa ON al.performed_by = sa.id
   JOIN store s ON al.store_id = s.id
   WHERE al.performed_at BETWEEN :startDate AND :endDate+1
     AND (:actionType IS NULL OR al.action_type = :actionType)
   ORDER BY al.performed_at DESC
   LIMIT :size OFFSET :page * :size
   ```

4. 전체 건수 조회 (페이징용)
   ```sql
   SELECT COUNT(*) FROM admin_audit_log al
   WHERE al.performed_at BETWEEN :startDate AND :endDate+1
     AND (:actionType IS NULL OR al.action_type = :actionType)
   ```

**Response (200 OK)**:
```json
{
  "logs": [
    {
      "logId": 5,
      "performerUsername": "superadmin",
      "targetAdminId": 10,
      "targetUsername": "admin1",
      "storeId": 1,
      "storeName": "맛있는 식당",
      "actionType": "CREATED",
      "performedAt": "2026-03-05T10:00:00"
    },
    {
      "logId": 4,
      "performerUsername": "superadmin",
      "targetAdminId": 8,
      "targetUsername": "oldadmin",
      "storeId": 2,
      "storeName": "좋은 카페",
      "actionType": "DELETED",
      "performedAt": "2026-03-04T15:30:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1
}
```

**에러 응답**:
| HTTP Status | 조건 | 메시지 |
|---|---|---|
| 400 | 날짜 형식 오류 | "유효하지 않은 날짜 형식입니다" |
| 400 | startDate > endDate | "시작일이 종료일보다 클 수 없습니다" |
| 400 | 유효하지 않은 actionType | "유효하지 않은 액션 유형입니다" |

**비즈니스 규칙**:
- AdminAuditLog는 DELETE 불가 (append-only, BR-05-03)
- 삭제된 관리자의 username은 targetUsername 스냅샷으로 보존 (BR-05-04)
- 이 엔드포인트는 조회 전용 — 이력 데이터 수정/삭제 API는 제공하지 않음


---

## 7. SSE 실시간 이벤트 흐름

### 7.1 SSE 연결 구독

**엔드포인트**: `GET /api/admin/sse/subscribe`
**인증**: Admin JWT (storeId)
**Content-Type**: `text/event-stream`

**처리 로직**:
1. 토큰에서 storeId 추출
2. SseEmitter 생성
   - timeout: 30분 (1,800,000ms)
   - 만료 후 자동 재연결 (클라이언트 EventSource가 자동 처리)

3. 연결 등록
   - 매장별 연결 풀: `Map<Long, List<SseEmitter>>` (storeId → emitters)
   - 새 emitter를 해당 storeId 리스트에 추가
   - 연결 ID 생성: `UUID.randomUUID()`

4. 초기 이벤트 전송 (연결 확인용)
   ```
   event: CONNECTED
   data: {"eventType":"CONNECTED","storeId":1,"connectedAt":"2026-03-05T12:00:00"}
   ```

5. 콜백 등록
   - `onCompletion`: emitter 리스트에서 제거
   - `onTimeout`: emitter 리스트에서 제거
   - `onError`: emitter 리스트에서 제거, 에러 로깅

6. Heartbeat (연결 유지)
   - 15초 간격으로 comment 전송: `: heartbeat`
   - 연결 끊김 감지 시 emitter 제거

**Response**: SSE 스트림 (연결 유지)
```
event: CONNECTED
data: {"eventType":"CONNECTED","storeId":1,"connectedAt":"2026-03-05T12:00:00"}

: heartbeat

event: NEW_ORDER
data: {"eventType":"NEW_ORDER","orderId":100,...}
```

---

### 7.2 이벤트 발행 (내부 메서드)

**메서드**: `SSEComponent.publishOrderEvent(storeId, event)`
**호출자**: OrderService (주문 생성/상태변경/삭제 시), TableSessionService (세션 종료 시)

**처리 로직**:
1. storeId로 연결 풀에서 emitter 리스트 조회
   - 리스트 없음 또는 비어있음 → 이벤트 발행 스킵 (로그만 기록)

2. 각 emitter에 이벤트 전송
   ```java
   for (SseEmitter emitter : emitters) {
       try {
           emitter.send(SseEmitter.event()
               .name(event.getEventType())
               .data(event, MediaType.APPLICATION_JSON));
       } catch (IOException e) {
           // 전송 실패한 emitter 제거 (dead connection)
           deadEmitters.add(emitter);
       }
   }
   ```

3. Dead emitter 정리
   - 전송 실패한 emitter를 리스트에서 제거

**이벤트 타입 및 데이터 구조**:

| 이벤트 타입 | 트리거 | 데이터 |
|---|---|---|
| `CONNECTED` | SSE 연결 성공 | storeId, connectedAt |
| `NEW_ORDER` | 주문 생성 (2.1) | orderId, orderNumber, tableNumber, totalAmount, itemCount, itemSummary, createdAt |
| `ORDER_STATUS_CHANGED` | 주문 상태 변경 (2.2) | orderId, orderNumber, tableNumber, previousStatus, newStatus, updatedAt |
| `ORDER_DELETED` | 주문 삭제 (2.3) | orderId, orderNumber, tableNumber, deletedAmount, deletedAt |
| `SESSION_COMPLETED` | 이용 완료 (3.2) | tableId, tableNumber, sessionId, completedAt, totalOrderAmount, orderCount |

**동시성 처리**:
- emitter 리스트는 `CopyOnWriteArrayList` 사용 (thread-safe)
- 이벤트 발행은 비동기 처리 불필요 (SSE 자체가 non-blocking)
- 연결 풀 정리는 콜백 기반으로 자동 처리

**에러 처리**:
- 개별 emitter 전송 실패는 해당 emitter만 제거 (다른 연결에 영향 없음)
- 연결 풀 전체 장애 시에도 주문 처리 로직은 정상 진행 (SSE는 부가 기능)
- SSE 전송 실패가 주문 트랜잭션을 롤백하지 않음

---

### 7.3 SSE 연결 관리 정책

**연결 제한**:
- 매장당 최대 동시 연결: 10개
- 초과 시 가장 오래된 연결 종료 후 새 연결 수락
- 연결 수 초과 → 기존 emitter `complete()` 호출 후 제거

**재연결 정책**:
- 클라이언트 `EventSource`의 자동 재연결 활용
- 서버 측 timeout (30분) 후 클라이언트가 자동 재연결
- 재연결 시 새로운 emitter 생성 (상태 없음, stateless)

**메모리 관리**:
- 주기적 정리 (5분 간격): 만료된 emitter 제거
- 서버 종료 시: 모든 emitter `complete()` 호출
- GC 친화적: dead emitter 참조 즉시 제거