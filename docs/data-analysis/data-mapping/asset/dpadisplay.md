# DPADISPLAY

배치된 자산 컴퓨터 디스플레이

> Target: MAXIMO.DPADISPLAY · ASSETCLASS: COMPUTER · 구현: DpaDisplayIntegrate.java

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : <1|N> DPADISPLAY  <!-- MERGE 키로 확정한다 -->
- 선행: DEPLOYEDASSET

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
|  | MAXIMO.DPADISPLAY |  |  |

Device42 에 대응 원천이 없다. 모니터 정보를 담는 뷰와 데이터가 확인되지
않았다. 다른 수집 도구가 채우는 영역이다.

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
|  |  |  |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| ASSETTAG | 자산 태그 | ALN(64) | Y |  |  |  |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N |  |  |  |
| COLORDEPTHBIT | 색상 수(비트) | INTEGER(12) | Y |  |  |  |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N |  |  |  |
| DESCRIPTION | 설명 | ALN(256) | Y |  |  |  |
| DISPLAYID | 디스플레이 ID | BIGINT(19) | N |  |  |  |
| DISPLAYSIZE | 디스플레이 크기 | INTEGER(12) | Y |  |  |  |
| DISPLAYTYPE | 디스플레이 유형 | ALN(32) | Y |  |  |  |
| MAKEMODEL | 제조/모델 | ALN(128) | Y |  |  |  |
| MANUFACTURER | 제조업체 | ALN(128) | N |  |  | DEFAULTVALUE=UNKNOWN |
| MAXHORZRESOLUTION | 최대 가로 해상도 | INTEGER(12) | Y |  |  |  |
| MAXVERTRESOLUTION | 최대 세로 해상도 | INTEGER(12) | Y |  |  |  |
| NODEID | 노드 ID | BIGINT(19) | N |  |  |  |
| SERIALNUMBER | 일련 번호 | ALN(64) | Y |  |  |  |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

3번 조건이 반영된, Device42 에서 원천을 끌어오는 SELECT 를 둔다.

## 6. 미결

`../../open-issues.md` 의 이슈 ID와 한 줄 요약만 둔다.

