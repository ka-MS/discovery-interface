# DPAMEDIAADAPTER

배치된 자산 컴퓨터 미디어 어댑터

> Target: MAXIMO.DPAMEDIAADAPTER · ASSETCLASS: COMPUTER · 구현: DpaMediaAdapterIntegrate.java

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : <1|N> DPAMEDIAADAPTER  <!-- MERGE 키로 확정한다 -->
- 선행: DEPLOYEDASSET

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
|  | MAXIMO.DPAMEDIAADAPTER |  |  |

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
|  |  |  |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| ADAPTERID | 어댑터 | BIGINT(19) | N |  |  |  |
| ASSETTAG | 자산 태그 | ALN(64) | Y |  |  |  |
| BUSTYPE | 버스 유형 | ALN(32) | Y |  |  |  |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N |  |  |  |
| CHIPSET | 칩셋 | ALN(64) | Y |  |  |  |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N |  |  |  |
| DESCRIPTION | 설명 | ALN(256) | Y |  |  |  |
| MAKEMODEL | 제조/모델 | ALN(128) | N |  |  | DEFAULTVALUE=UNKNOWN |
| MANUFACTURER | 제조업체 | ALN(128) | N |  |  | DEFAULTVALUE=UNKNOWN |
| MEDIATYPE | 미디어 어댑터 유형 | ALN(32) | Y |  |  |  |
| MEMORYTYPE | 메모리 유형 | ALN(32) | Y |  |  |  |
| NODEID | 노드 ID | BIGINT(19) | N |  |  |  |
| RAMSIZE | RAM 크기 | DECIMAL(10,2) | Y |  |  |  |
| RAMUNIT | RAM 단위 | ALN(16) | Y |  |  |  |
| SERIALNUMBER | 일련 번호 | ALN(64) | Y |  |  |  |
| VRAMSIZE | RAM 크기 | ALN(32) | Y |  |  |  |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

3번 조건이 반영된, Device42 에서 원천을 끌어오는 SELECT 를 둔다.

## 6. 미결

`../../open-issues.md` 의 이슈 ID와 한 줄 요약만 둔다.

