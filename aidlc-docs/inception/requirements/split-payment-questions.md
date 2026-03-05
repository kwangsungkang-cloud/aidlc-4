# 주문 분할 계산 기능 명확화 질문

FR-04 주문 생성에 "분할 계산" 기능 추가 요청에 대해 명확화가 필요합니다.
각 질문의 [Answer]: 태그 뒤에 선택한 옵션의 알파벳을 입력해 주세요.

---

## Question 1
"분할 계산"의 의미를 명확히 해주세요. 어떤 시점에 분할이 이루어지나요?

A) 주문 시점에 분할 — 주문 생성 시 각 사람이 자기 메뉴만 별도 주문으로 생성
B) 계산 시점에 분할 — 주문은 테이블 단위로 하되, 나중에 결제할 때 나눠서 계산 (금액 분할 표시)
C) 둘 다 — 주문도 분리 가능하고, 계산도 분할 가능
X) Other (please describe after [Answer]: tag below)

[Answer]: B

## Question 2
"메뉴별 분할"의 구체적인 동작은 무엇인가요?

A) 특정 메뉴 항목을 선택하여 별도 그룹으로 묶기 (예: A가 치킨+맥주, B가 피자+콜라)
B) 각 메뉴 항목에 담당자(이름/번호)를 지정
C) 단순히 총 금액에서 선택한 메뉴 항목의 합계를 별도로 보여주기
X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 3
"인원별 분할"의 구체적인 동작은 무엇인가요?

A) 총 금액을 N명으로 균등 분할 (1/N)
B) 각 인원에게 메뉴를 배정한 후 인원별 합계 계산
C) 균등 분할과 메뉴 배정 분할 모두 지원
X) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 4
결제 기능이 제외 범위(constraints.md)에 포함되어 있습니다. 분할 계산 결과는 어떻게 활용되나요?

A) 분할된 금액을 화면에 표시만 함 (실제 결제는 매장에서 별도 처리)
B) 분할된 금액별로 별도 주문으로 분리하여 관리자 화면에 표시
C) 분할 정보를 주문에 메모/태그로 첨부
X) Other (please describe after [Answer]: tag below)

[Answer]: A
