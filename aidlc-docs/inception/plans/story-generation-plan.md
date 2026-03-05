# Story Generation Plan — 테이블오더 서비스

## 실행 계획

### Phase 1: 페르소나 정의
- [x] 고객(Customer) 페르소나 정의
- [x] 매장 관리자(Store Admin) 페르소나 정의
- [x] 슈퍼 관리자(Super Admin) 페르소나 정의

### Phase 2: 고객용 User Stories
- [x] US-01: 테이블 태블릿 자동 로그인 (FR-01)
- [x] US-02: 메뉴 조회 및 탐색 (FR-02)
- [x] US-03: 장바구니 관리 (FR-03)
- [x] US-04: 주문 생성 (FR-04)
- [x] US-05: 분할 계산 (FR-04-1)
- [x] US-06: 주문 내역 조회 (FR-05)

### Phase 3: 매장 관리자용 User Stories
- [x] US-07: 매장 관리자 로그인 (FR-06)
- [x] US-08: 실시간 주문 모니터링 (FR-07)
- [x] US-09: 주문 상태 변경 (FR-07)
- [x] US-10: 테이블 초기 설정 (FR-08)
- [x] US-11: 주문 삭제 (FR-08)
- [x] US-12: 테이블 이용 완료 (FR-08)
- [x] US-13: 과거 주문 내역 조회 (FR-08)
- [x] US-14: 메뉴 관리 (FR-09)

### Phase 4: 슈퍼 관리자용 User Stories
- [x] US-15: 매장 관리자 계정 관리 (FR-10)
- [x] US-16: 활동 이력 조회 (FR-11)

### Phase 5: 검증
- [x] INVEST 기준 검증
- [x] 페르소나-스토리 매핑 검증
- [x] 수용 기준 완전성 검증

---

## 명확화 질문

아래 질문에 답변해 주세요. 각 질문의 [Answer]: 태그 뒤에 선택한 옵션의 알파벳을 입력해 주세요.

### Question 1
User Story의 분류 방식은 어떤 것을 선호하시나요?

A) 페르소나 기반 — 사용자 유형별로 그룹화 (고객 스토리, 관리자 스토리, 슈퍼 관리자 스토리)
B) 기능 기반 — 시스템 기능별로 그룹화 (주문 관련, 메뉴 관련, 테이블 관련)
C) 사용자 여정 기반 — 워크플로우 순서대로 (입장→메뉴조회→주문→확인)
X) Other (please describe after [Answer]: tag below)

[Answer]: C

### Question 2
수용 기준(Acceptance Criteria)의 상세 수준은 어느 정도를 원하시나요?

A) 간결 — 핵심 조건만 3~5개 (예: "메뉴가 카테고리별로 표시된다")
B) 상세 — Given/When/Then 형식으로 시나리오별 기술 (예: "Given 고객이 메뉴 화면에 있을 때, When 카테고리를 선택하면, Then 해당 카테고리의 메뉴만 표시된다")
C) 혼합 — 핵심 스토리는 상세, 단순 스토리는 간결
X) Other (please describe after [Answer]: tag below)

[Answer]: B

### Question 3
스토리 우선순위를 어떤 기준으로 설정하시겠습니까?

A) MoSCoW (Must/Should/Could/Won't)
B) 높음/중간/낮음 (High/Medium/Low)
C) 숫자 우선순위 (P1, P2, P3)
X) Other (please describe after [Answer]: tag below)

[Answer]: C

### Question 4
고객 페르소나를 어떻게 세분화하시겠습니까?

A) 단일 페르소나 — "식당 고객" 하나로 통합
B) 연령대별 세분화 — 디지털 친숙 고객 vs 디지털 비친숙 고객
C) 이용 패턴별 — 단독 고객 vs 그룹 고객 (분할 계산 관련)
X) Other (please describe after [Answer]: tag below)

[Answer]: B
