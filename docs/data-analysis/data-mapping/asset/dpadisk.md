# DPADISK

배치된 자산 컴퓨터 디스크

> Target: MAXIMO.DPADISK · ASSETCLASS: COMPUTER · 구현: DpaDiskIntegrate.java

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : N DPADISK (관측 61노드/144행)
- 선행: DEPLOYEDASSET

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
| `view_part_v1`(Hard Disk) | MAXIMO.DPADISK |  |  |

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
|  |  |  |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| ASSETTAG | 자산 태그 | ALN(64) | Y |  |  |  |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N |  |  |  |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N |  |  |  |
| DESCRIPTION | 설명 | ALN(256) | Y |  |  |  |
| DISKID | 디스크 ID | BIGINT(19) | N |  |  |  |
| DISKINTERFACE | 디스크 인터페이스 | ALN(32) | Y |  |  |  |
| DISKTYPE | 디스크 유형 | ALN(32) | Y |  |  |  |
| EXTERNALDEVICE | 외부 디바이스 | YORN(1) | N |  |  |  |
| HOTSWAPPABLE | 핫스왑 가능 | YORN(1) | N |  |  |  |
| MAKEMODEL | 제조/모델 | ALN(128) | Y |  |  |  |
| MANUFACTURER | 제조업체 | ALN(128) | N |  |  | DEFAULTVALUE=UNKNOWN |
| NODEID | 노드 ID | BIGINT(19) | N |  |  |  |
| REMOVABLEMEDIA | 이동식 미디어 | YORN(1) | N |  |  |  |
| SERIALNUMBER | 일련 번호 | ALN(64) | Y |  |  |  |
| SIZEUNIT | 크기 단위 | ALN(16) | Y |  |  |  |
| SYSTEMNAME | 디바이스 | ALN(64) | Y |  |  |  |
| TOTALSPACE | 총 공간 | DECIMAL(10,2) | Y |  |  |  |
| VTOTALSPACE | 크기 | ALN(32) | Y |  |  |  |
| WRITECAPABLE | 쓰기 가능 | YORN(1) | N |  |  |  |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

3번 조건이 반영된, Device42 에서 원천을 끌어오는 SELECT 를 둔다.

## 6. 미결

`../../open-issues.md` 의 이슈 ID와 한 줄 요약만 둔다.

