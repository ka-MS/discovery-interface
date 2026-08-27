# DPANETPRINTER

배치된 자산 네트워크 프린터

> Target: MAXIMO.DPANETPRINTER · ASSETCLASS: NETPRINTER · 구현: DpaNetPrinterIntegrate.java

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : 1 DPANETPRINTER (PK 가 `NODEID` 단독. 관측 4노드/4행)
- 선행: DEPLOYEDASSET

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
| `view_device_v2`, `view_part_v1`(printer_*) | MAXIMO.DPANETPRINTER |  |  |

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
|  |  |  |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N |  |  |  |
| COLORDEPTHBIT | 색상 수(비트) | INTEGER(12) | Y |  |  |  |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N |  |  |  |
| CURRENTRAM | 현재 RAM 크기 | DECIMAL(10,2) | Y |  |  |  |
| HORIZONTALDPI | 가로 DPI | INTEGER(12) | Y |  |  |  |
| MAXLENGTH | 최대 용지 길이 | DECIMAL(10,2) | Y |  |  |  |
| MAXRAM | 최대 RAM | DECIMAL(10,2) | Y |  |  |  |
| MAXWIDTH | 최대 용지 너비 | DECIMAL(10,2) | Y |  |  |  |
| NETMACADDR | 네트워크 MAC 주소 | ALN(17) | Y |  |  |  |
| NETWORKADDRESS | 네트워크 주소 | ALN(39) | Y |  |  |  |
| NODEID | 노드 ID | BIGINT(19) | N |  |  |  |
| NUMBEROFTRAYS | 용지함 수 | INTEGER(12) | Y |  |  |  |
| RAMUNIT | RAM 단위 | ALN(16) | Y |  |  |  |
| SIZEUNIT | 크기 단위 | ALN(16) | Y |  |  |  |
| VCURRENTRAMSIZE | 현재 RAM 크기 | ALN(32) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |
| VERTICALDPI | 세로 DPI | INTEGER(12) | Y |  |  |  |
| VMAXLENGTH | 최대 길이 | ALN(32) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |
| VMAXRAMSIZE | 최대 RAM 크기 | ALN(32) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |
| VMAXWIDTH | 최대 너비 | ALN(32) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

3번 조건이 반영된, Device42 에서 원천을 끌어오는 SELECT 를 둔다.

## 6. 미결

`../../open-issues.md` 의 이슈 ID와 한 줄 요약만 둔다.

