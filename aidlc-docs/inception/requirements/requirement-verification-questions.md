# 요구사항 명확화 질문

아래 질문에 답변해 주세요. 각 질문의 [Answer]: 태그 뒤에 선택한 옵션의 알파벳을 입력해 주세요.
해당하는 옵션이 없으면 마지막 옵션(Other)을 선택하고 설명을 추가해 주세요.

---

## Question 1
백엔드 기술 스택으로 어떤 것을 사용하시겠습니까?

A) Node.js + Express (JavaScript/TypeScript)
B) Spring Boot (Java/Kotlin)
C) Django/FastAPI (Python)
D) NestJS (TypeScript)
X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 2
프론트엔드 기술 스택으로 어떤 것을 사용하시겠습니까?

A) React (TypeScript)
B) Vue.js (TypeScript)
C) Next.js (TypeScript, SSR)
D) Svelte/SvelteKit
X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 3
데이터베이스로 어떤 것을 사용하시겠습니까?

A) PostgreSQL
B) MySQL
C) MongoDB
D) SQLite (개발/소규모 매장용)
X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 4
배포 환경은 어떻게 계획하고 계십니까?

A) 클라우드 (AWS)
B) 클라우드 (GCP, Azure 등)
C) 자체 서버 / On-premise
D) 배포 환경은 아직 미정 (로컬 개발 환경만 우선 구축)
X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 5
고객용 인터페이스와 관리자용 인터페이스를 어떻게 구성하시겠습니까?

A) 하나의 프론트엔드 앱에서 라우팅으로 분리
B) 별도의 프론트엔드 앱 2개 (고객용 + 관리자용)
C) 고객용은 모바일 웹 최적화, 관리자용은 데스크톱 웹
X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 6
메뉴 이미지 관리는 어떻게 하시겠습니까? (요구사항에 이미지 URL로 명시되어 있습니다)

A) 외부 이미지 URL 직접 입력 (별도 업로드 없음)
B) 서버에 이미지 파일 업로드 후 URL 자동 생성
C) 클라우드 스토리지(S3 등)에 업로드 후 URL 자동 생성
X) Other (please describe after [Answer]: tag below)

[Answer]: C

## Question 7
동시 접속 규모는 어느 정도를 예상하십니까? (매장 수 기준)

A) 단일 매장 (1개 매장, 테이블 10~30개)
B) 소규모 (2~10개 매장)
C) 중규모 (10~50개 매장)
D) 대규모 (50개 이상 매장, 멀티테넌트)
X) Other (please describe after [Answer]: tag below)

[Answer]: C

## Question 8
관리자 계정 관리는 어떻게 하시겠습니까?

A) 시스템에서 직접 관리자 계정 생성 (회원가입 기능 포함)
B) 사전 등록된 관리자 계정만 사용 (DB에 직접 등록)
C) 슈퍼 관리자가 매장 관리자 계정을 생성/관리
X) Other (please describe after [Answer]: tag below)

[Answer]: C

## Question 9
메뉴 관리 기능은 MVP에 포함하시겠습니까? (요구사항 3.2.4에 정의되어 있으나 MVP 범위 섹션에는 명시되지 않았습니다)

A) 예, MVP에 포함 (관리자가 메뉴 CRUD 가능)
B) 아니오, MVP에서 제외 (DB에 직접 메뉴 데이터 입력)
X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 10: Security Extensions
이 프로젝트에 보안 확장 규칙(SECURITY rules)을 적용하시겠습니까?

A) 예 — 모든 SECURITY 규칙을 blocking constraint로 적용 (프로덕션 수준 애플리케이션에 권장)
B) 아니오 — SECURITY 규칙 건너뛰기 (PoC, 프로토타입, 실험적 프로젝트에 적합)
X) Other (please describe after [Answer]: tag below)

[Answer]: A
