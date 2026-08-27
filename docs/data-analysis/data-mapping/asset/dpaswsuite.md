# DPASWSUITE

배치된 자산 컴퓨터 스위트

> Target: MAXIMO.DPASWSUITE · ASSETCLASS: COMPUTER · 구현: DpaSwSuiteIntegrate.java
> 관측 2026-08-27 · Device42 192.168.1.35 / Maximo BLUDB

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : N DPASWSUITE (PK 는 `DPASWSUITEID`. 관측은 14노드/14행이나 스키마는 다건을 허용한다)
- 선행: DEPLOYEDASSET

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
| 없음 | MAXIMO.DPASWSUITE | – | 적재 행 없음 |

Device42 에 대응 원천이 없다. 소프트웨어 여러 개를 스위트 하나로 묶는 개념과
뷰가 확인되지 않았다. 다른 수집 도구가 채우는 영역이다.

확인한 근거는 세 가지다.

- 스위트 뷰가 없다. `view_softwaresuite_v1`, `view_suite_v1`,
  `view_softwarecomponent_v1` 모두 500 을 반환한다. DOQL 에서 500 은 뷰명
  또는 권한 오류를 뜻한다. `knowledge/device42/doql-constraints.md` 참조.
- `view_software_v1` 에 묶음 컬럼이 없다. `category_name` 은 807/808 이 비어
  있고 `server_software_fk` 는 0/808 이라 스위트 구성원 관계를 담지 않는다.
- 기존 수집분 14행은 전부 `MSOFFICE` / `Office XP Standard` 다. Windows 인벤토리
  수집기의 스위트 개념이며 Device42 수집 범위와 겹치지 않는다.

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
| 원천 없음 | – | Device42에 대응 뷰와 데이터가 없다 |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N | 원천없음 | – | 대응 원천이 없어 DPASWSUITE 행을 생성하지 않는다 |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N | 원천없음 | – | 대응 원천이 없어 DPASWSUITE 행을 생성하지 않는다 |
| DESCRIPTION | 설명 | ALN(256) | Y | 원천없음 | – | 대응 원천이 없다 |
| DPASWSUITEID | 고유 ID | BIGINT(19) | N | 원천없음 | – | 대응 원천이 없어 채번 대상 행이 없다 |
| INSTALLDATE | 설치 날짜 | DATETIME(10) | Y | 원천없음 | – | 대응 원천이 없다 |
| LANGUAGE | 언어 | ALN(32) | Y | 원천없음 | – | 대응 원천이 없다 |
| LASTUSAGEDATE | 최종 사용일 | DATETIME(10) | Y | 원천없음 | – | 대응 원천이 없다 |
| LICENSEDORG | 라이센스가 부여된 조직 | ALN(64) | Y | 원천없음 | – | 대응 원천이 없다 |
| LICENSEDUSER | 라이센스가 부여된 사용자 | ALN(64) | Y | 원천없음 | – | 대응 원천이 없다 |
| MANUFACTURER | 제조업체 | ALN(128) | N | 원천없음 | – | 대응 원천이 없어 행을 생성하지 않는다. DEFAULTVALUE=UNKNOWN |
| NODEID | 노드 ID | BIGINT(19) | N | 원천없음 | – | 대응 원천이 없어 부모와 연결할 행이 없다 |
| PRODUCTID | 제품 ID | ALN(128) | Y | 원천없음 | – | 대응 원천이 없다 |
| SERIALNUMBER | 일련 번호 | ALN(64) | Y | 원천없음 | – | 대응 원천이 없다 |
| SUITEID | 스위트 ID | BIGINT(19) | N | 원천없음 | – | 대응 원천이 없어 채번 대상 행이 없다 |
| SUITENAME | 스위트 | ALN(256) | Y | 원천없음 | – | 대응 원천이 없다 |
| USAGECOUNT | 사용 회수 | INTEGER(12) | Y | 원천없음 | – | 대응 원천이 없다 |
| VERSION | 버전 | ALN(64) | Y | 원천없음 | – | 대응 원천이 없다 |
| VUSAGEDISPLAYTEXT | 사용 | ALN(64) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

원천이 없어 조회 쿼리가 없다. 사유는 2번을 참조한다.

## 6. 미결

없음. Device42 원천이 생기기 전까지 적재 대상에서 제외한다.
