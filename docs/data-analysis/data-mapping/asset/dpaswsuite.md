# DPASWSUITE

배치된 자산 컴퓨터 스위트

> Target: MAXIMO.DPASWSUITE · ASSETCLASS: COMPUTER · 구현: DpaSwSuiteIntegrate.java

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : N DPASWSUITE (PK 는 `DPASWSUITEID`, 유니크 인덱스는 `(SUITEID, NODEID)`. 관측은 14노드/14행이나 스키마는 다건을 허용한다)
- 선행: DEPLOYEDASSET

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
| 없음 | MAXIMO.DPASWSUITE |  |  |

Device42 에 대응 원천이 없다. 소프트웨어를 스위트로 묶는 개념과 뷰가
확인되지 않았다. 다른 수집 도구가 채우는 영역이다.

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
|  |  |  |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N |  |  |  |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N |  |  |  |
| DESCRIPTION | 설명 | ALN(256) | Y |  |  |  |
| DPASWSUITEID | 고유 ID | BIGINT(19) | N |  |  |  |
| INSTALLDATE | 설치 날짜 | DATETIME(10) | Y |  |  |  |
| LANGUAGE | 언어 | ALN(32) | Y |  |  |  |
| LASTUSAGEDATE | 최종 사용일 | DATETIME(10) | Y |  |  |  |
| LICENSEDORG | 라이센스가 부여된 조직 | ALN(64) | Y |  |  |  |
| LICENSEDUSER | 라이센스가 부여된 사용자 | ALN(64) | Y |  |  |  |
| MANUFACTURER | 제조업체 | ALN(128) | N |  |  | DEFAULTVALUE=UNKNOWN |
| NODEID | 노드 ID | BIGINT(19) | N |  |  |  |
| PRODUCTID | 제품 ID | ALN(128) | Y |  |  |  |
| SERIALNUMBER | 일련 번호 | ALN(64) | Y |  |  |  |
| SUITEID | 스위트 ID | BIGINT(19) | N |  |  |  |
| SUITENAME | 스위트 | ALN(256) | Y |  |  |  |
| USAGECOUNT | 사용 회수 | INTEGER(12) | Y |  |  |  |
| VERSION | 버전 | ALN(64) | Y |  |  |  |
| VUSAGEDISPLAYTEXT | 사용 | ALN(64) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

원천이 없어 조회 쿼리가 없다. 사유는 2번을 참조한다.

## 6. 미결

`../../open-issues.md` 의 이슈 ID와 한 줄 요약만 둔다.

