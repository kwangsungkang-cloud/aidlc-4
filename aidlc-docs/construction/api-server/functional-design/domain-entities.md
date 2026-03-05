# Domain Entities — api-server

---

## Entity Relationship Diagram (Text)

```
Store 1──* Table 1──* TableSession 1──* Order 1──* OrderItem *──1 Menu
  |                                                                  |
  1──* Admin                                                    *──1 Category
  |
  1──* Category
  
SuperAdmin 1──* AdminAuditLog
```

---

## 1. Store (매장)

| 속성 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | Long | PK, auto | 매장 ID |
| storeCode | String | UNIQUE, NOT NULL, max 50 | 매장 식별 코드 |
| storeName | String | NOT NULL, max 100 | 매장명 |
| createdAt | LocalDateTime | NOT NULL | 생성 시각 |
| updatedAt | LocalDateTime | NOT NULL | 수정 시각 |

---

## 2. StoreTable (테이블)

| 속성 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | Long | PK, auto | 테이블 ID |
| storeId | Long | FK(Store), NOT NULL | 소속 매장 |
| tableNumber | Integer | NOT NULL | 테이블 번호 |
| password | String | NOT NULL (bcrypt) | 테이블 비밀번호 |
| createdAt | LocalDateTime | NOT NULL | 생성 시각 |

**제약**: (storeId, tableNumber) UNIQUE

---

## 3. TableSession (테이블 세션)

| 속성 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | Long | PK, auto | 세션 ID |
| tableId | Long | FK(StoreTable), NOT NULL | 소속 테이블 |
| sessionToken | String | UNIQUE, NOT NULL | 세션 토큰 |
| status | Enum | NOT NULL (ACTIVE/COMPLETED) | 세션 상태 |
| startedAt | LocalDateTime | NOT NULL | 세션 시작 시각 |
| completedAt | LocalDateTime | nullable | 이용 완료 시각 |
| expiresAt | LocalDateTime | NOT NULL | 만료 시각 (16시간) |

---

## 4. Category (메뉴 카테고리)

| 속성 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | Long | PK, auto | 카테고리 ID |
| storeId | Long | FK(Store), NOT NULL | 소속 매장 |
| name | String | NOT NULL, max 50 | 카테고리명 |
| displayOrder | Integer | NOT NULL, default 0 | 노출 순서 |

---

## 5. Menu (메뉴)

| 속성 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | Long | PK, auto | 메뉴 ID |
| storeId | Long | FK(Store), NOT NULL | 소속 매장 |
| categoryId | Long | FK(Category), NOT NULL | 소속 카테고리 |
| name | String | NOT NULL, max 100 | 메뉴명 |
| price | Integer | NOT NULL, min 0 | 가격 (원) |
| description | String | nullable, max 500 | 메뉴 설명 |
| imageUrl | String | nullable, max 500 | 이미지 URL (S3) |
| displayOrder | Integer | NOT NULL, default 0 | 노출 순서 |
| createdAt | LocalDateTime | NOT NULL | 생성 시각 |
| updatedAt | LocalDateTime | NOT NULL | 수정 시각 |

---

## 6. Order (주문)

| 속성 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | Long | PK, auto | 주문 ID |
| sessionId | Long | FK(TableSession), NOT NULL | 소속 세션 |
| orderNumber | String | UNIQUE, NOT NULL | 주문 번호 (표시용) |
| status | Enum | NOT NULL (PENDING/PREPARING/COMPLETED) | 주문 상태 |
| totalAmount | Integer | NOT NULL, min 0 | 총 금액 |
| createdAt | LocalDateTime | NOT NULL | 주문 시각 |
| updatedAt | LocalDateTime | NOT NULL | 수정 시각 |

---

## 7. OrderItem (주문 항목)

| 속성 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | Long | PK, auto | 항목 ID |
| orderId | Long | FK(Order), NOT NULL | 소속 주문 |
| menuId | Long | FK(Menu), NOT NULL | 메뉴 참조 |
| menuName | String | NOT NULL | 주문 시점 메뉴명 (스냅샷) |
| quantity | Integer | NOT NULL, min 1 | 수량 |
| unitPrice | Integer | NOT NULL, min 0 | 주문 시점 단가 (스냅샷) |
| subtotal | Integer | NOT NULL | 소계 (quantity * unitPrice) |

---

## 8. Admin (매장 관리자)

| 속성 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | Long | PK, auto | 관리자 ID |
| storeId | Long | FK(Store), NOT NULL | 소속 매장 |
| username | String | NOT NULL, max 50 | 사용자명 |
| password | String | NOT NULL (bcrypt) | 비밀번호 |
| loginAttempts | Integer | NOT NULL, default 0 | 연속 로그인 실패 횟수 |
| lockedUntil | LocalDateTime | nullable | 잠금 해제 시각 |
| createdAt | LocalDateTime | NOT NULL | 생성 시각 |

**제약**: (storeId, username) UNIQUE

---

## 9. SuperAdmin (슈퍼 관리자)

| 속성 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | Long | PK, auto | 슈퍼 관리자 ID |
| username | String | UNIQUE, NOT NULL, max 50 | 사용자명 |
| password | String | NOT NULL (bcrypt) | 비밀번호 |
| createdAt | LocalDateTime | NOT NULL | 생성 시각 |

---

## 10. AdminAuditLog (관리자 활동 이력)

| 속성 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | Long | PK, auto | 이력 ID |
| performedBy | Long | FK(SuperAdmin), NOT NULL | 수행자 |
| targetAdminId | Long | nullable | 대상 관리자 ID |
| targetUsername | String | NOT NULL | 대상 사용자명 (스냅샷) |
| storeId | Long | FK(Store), NOT NULL | 대상 매장 |
| actionType | Enum | NOT NULL (CREATED/DELETED) | 액션 유형 |
| performedAt | LocalDateTime | NOT NULL | 수행 시각 |

**제약**: DELETE 불가 (append-only)
