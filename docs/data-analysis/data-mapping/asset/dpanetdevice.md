# DPANETDEVICE

배치된 자산 네트워크 디바이스

> Target: MAXIMO.DPANETDEVICE · ASSETCLASS: NETDEVICE · 구현: DpaNetDeviceIntegrate.java

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : <1|N> DPANETDEVICE  <!-- MERGE 키로 확정한다 -->
- 선행: DEPLOYEDASSET

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
|  | MAXIMO.DPANETDEVICE |  |  |

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
|  |  |  |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N |  |  |  |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N |  |  |  |
| DESCRIPTION1 | 설명 | ALN(128) | Y |  |  |  |
| FIRMWAREVERSION | 펌웨어 버전 | ALN(128) | Y |  |  |  |
| NETMACADDR | MAC 주소 | ALN(17) | Y |  |  |  |
| NETSOURCEID1 | 네트워크 소스 ID | ALN(128) | Y |  |  |  |
| NETWORKADDRESS | 네트워크 주소 | ALN(39) | Y |  |  |  |
| NODEID | 노드 ID | BIGINT(19) | N |  |  |  |
| OSVERSION | 운영 체제 버전 | ALN(128) | Y |  |  |  |
| RAMSIZE | RAM 크기 | DECIMAL(10,2) | Y |  |  |  |
| RAMUNIT | RAM 단위 | ALN(16) | Y |  |  |  |
| VRAMSIZE | RAM 크기 | ALN(32) | Y |  |  |  |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

3번 조건이 반영된, Device42 에서 원천을 끌어오는 SELECT 를 둔다.

## 6. 미결

`../../open-issues.md` 의 이슈 ID와 한 줄 요약만 둔다.

