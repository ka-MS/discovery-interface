# DPADISPLAY

배치된 자산 컴퓨터 디스플레이

> Target: MAXIMO.DPADISPLAY · ASSETCLASS: COMPUTER · 구현: DpaDisplayIntegrate.java
> 관측 2026-08-27 · Device42 192.168.1.35, 192.168.2.68 / Maximo BLUDB

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : N DPADISPLAY (PK 는 `DISPLAYID`. 관측 37노드/52행)
- 선행: DEPLOYEDASSET

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
| 없음 | MAXIMO.DPADISPLAY | – | 적재 행 없음 |

두 Device42 서버 모두 `view_display_v1`, `view_monitor_v1`이 존재하지 않는다.
모니터 정보를 담는 다른 대응 뷰와 데이터도 확인되지 않았다. 다른 수집 도구가
채우는 영역이다.

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
| 원천 없음 | – | 두 Device42 서버 모두 대응 뷰와 데이터가 없다 |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| ASSETTAG | 자산 태그 | ALN(64) | Y | 원천없음 | – | 대응 원천이 없어 DPADISPLAY 행을 생성하지 않는다 |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N | 원천없음 | – | 대응 원천이 없어 DPADISPLAY 행을 생성하지 않는다 |
| COLORDEPTHBIT | 색상 수(비트) | INTEGER(12) | Y | 원천없음 | – | 대응 원천이 없다 |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N | 원천없음 | – | 대응 원천이 없어 DPADISPLAY 행을 생성하지 않는다 |
| DESCRIPTION | 설명 | ALN(256) | Y | 원천없음 | – | 대응 원천이 없다 |
| DISPLAYID | 디스플레이 ID | BIGINT(19) | N | 원천없음 | – | 대응 시퀀스는 `MAXIMO.DPADISPLAYSEQ`. 원천이 없어 현재는 채번 대상 행이 없다 |
| DISPLAYSIZE | 디스플레이 크기 | INTEGER(12) | Y | 원천없음 | – | 대응 원천이 없다 |
| DISPLAYTYPE | 디스플레이 유형 | ALN(32) | Y | 원천없음 | – | 대응 원천이 없다 |
| MAKEMODEL | 제조/모델 | ALN(128) | Y | 원천없음 | – | 대응 원천이 없다 |
| MANUFACTURER | 제조업체 | ALN(128) | N | 원천없음 | – | 대응 원천이 없어 행을 생성하지 않는다. DEFAULTVALUE=UNKNOWN |
| MAXHORZRESOLUTION | 최대 가로 해상도 | INTEGER(12) | Y | 원천없음 | – | 대응 원천이 없다 |
| MAXVERTRESOLUTION | 최대 세로 해상도 | INTEGER(12) | Y | 원천없음 | – | 대응 원천이 없다 |
| NODEID | 노드 ID | BIGINT(19) | N | 원천없음 | – | 대응 원천이 없어 부모와 연결할 행이 없다 |
| SERIALNUMBER | 일련 번호 | ALN(64) | Y | 원천없음 | – | 대응 원천이 없다 |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

원천이 없어 조회 쿼리가 없다. 사유는 2번을 참조한다.

## 6. 미결

없음. Device42 원천이 생기기 전까지 적재 대상에서 제외한다.
