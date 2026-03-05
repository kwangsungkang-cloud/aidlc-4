# 테이블오더 서비스 — Unit of Work 정의

---

## Unit 분해 전략
- **아키텍처**: 모놀리식 백엔드 + 프론트엔드 2개 앱 (모노레포)
- **분해 기준**: 배포 단위 및 기술 스택 기반
- **개발 순서**: Unit 1 (api-server) → Unit 2 (customer-web) + Unit 3 (admin-web) 병렬

---

## Unit 1: api-server (Spring Boot 백엔드)

| 항목 | 내용 |
|---|---|
| 기술 스택 | Spring Boot, Java/Kotlin, Spring Data JPA, MySQL, AWS S3 SDK |
| 배포 단위 | 단일 JAR/Docker 컨테이너 |
| 책임 | 모든 비즈니스 로직, REST API, SSE, 데이터 접근, 파일 스토리지 |

**포함 컴포넌트**:
- AuthComponent, StoreComponent, TableComponent
- MenuComponent, OrderComponent, AdminComponent
- SSEComponent, FileStorageComponent

**포함 서비스**:
- AuthService, TableSessionService, OrderService
- MenuService, AdminService, SSEService

**주요 산출물**:
- REST API 엔드포인트 (9개 그룹)
- MySQL 스키마 (11개 엔티티)
- SSE 실시간 이벤트 스트리밍
- S3 이미지 업로드
- JWT 인증/인가
- 보안 규칙 적용 (SECURITY-01~15)

---

## Unit 2: customer-web (고객용 React 앱)

| 항목 | 내용 |
|---|---|
| 기술 스택 | React, TypeScript, Vite, shadcn/ui, Tailwind CSS |
| 배포 단위 | 정적 파일 (S3 + CloudFront 또는 Nginx) |
| 책임 | 고객 주문 UI, 장바구니, 분할 계산 |

**포함 모듈**:
- AuthModule, WelcomeModule, MenuModule
- CartModule, OrderModule, SplitBillModule

**주요 산출물**:
- 태블릿 최적화 반응형 UI
- 자동 로그인 + 로컬 저장소 관리
- 초기 접속 안내 화면
- 장바구니 (localStorage)
- 분할 계산 (클라이언트 측)

---

## Unit 3: admin-web (관리자용 React 앱)

| 항목 | 내용 |
|---|---|
| 기술 스택 | React, TypeScript, Vite, shadcn/ui, Tailwind CSS |
| 배포 단위 | 정적 파일 (S3 + CloudFront 또는 Nginx) |
| 책임 | 관리자 대시보드, 주문/테이블/메뉴/계정 관리 |

**포함 모듈**:
- AuthModule, DashboardModule, OrderManagementModule
- TableManagementModule, MenuManagementModule, AdminManagementModule

**주요 산출물**:
- 데스크톱 최적화 대시보드 UI
- SSE 기반 실시간 주문 모니터링
- 테이블 관리 (초기 설정, 이용 완료, 과거 내역)
- 메뉴 CRUD + S3 이미지 업로드
- 슈퍼 관리자 계정/이력 관리

---

## 코드 조직 전략 (Greenfield 모노레포)

```
table-order/                    # 모노레포 루트
├── api-server/                 # Unit 1: Spring Boot
│   ├── build.gradle
│   └── src/
│       ├── main/
│       │   ├── java/com/tableorder/
│       │   │   ├── auth/       # 인증 도메인
│       │   │   ├── store/      # 매장 도메인
│       │   │   ├── table/      # 테이블 도메인
│       │   │   ├── menu/       # 메뉴 도메인
│       │   │   ├── order/      # 주문 도메인
│       │   │   ├── admin/      # 관리자 도메인
│       │   │   ├── sse/        # SSE 도메인
│       │   │   ├── storage/    # 파일 스토리지
│       │   │   └── common/     # 공통 (config, security, exception)
│       │   └── resources/
│       │       └── application.yml
│       └── test/
├── customer-web/               # Unit 2: React 고객용
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
│       ├── modules/
│       │   ├── auth/
│       │   ├── welcome/
│       │   ├── menu/
│       │   ├── cart/
│       │   ├── order/
│       │   └── split-bill/
│       ├── common/
│       └── App.tsx
├── admin-web/                  # Unit 3: React 관리자용
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
│       ├── modules/
│       │   ├── auth/
│       │   ├── dashboard/
│       │   ├── order-management/
│       │   ├── table-management/
│       │   ├── menu-management/
│       │   └── admin-management/
│       ├── common/
│       └── App.tsx
└── README.md
```

---

## 개발 순서

| 순서 | Unit | 이유 |
|---|---|---|
| 1 | api-server | 프론트엔드가 의존하는 API를 먼저 구축 |
| 2 | customer-web + admin-web (병렬) | API 완성 후 프론트엔드 병렬 개발 가능 |
