# DPANETDEVICE

배치된 자산 네트워크 디바이스

> Target: MAXIMO.DPANETDEVICE · ASSETCLASS: NETDEVICE · 구현: DpaNetDeviceIntegrate.java

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : 1 DPANETDEVICE (PK 가 `NODEID` 단독. 관측 34노드/34행)
- 선행: DEPLOYEDASSET

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
| `view_device_v2`(`physical`) | MAXIMO.DPANETDEVICE | – | 1:1 |
| MAXIMO.DEPLOYEDASSET | (교차키) | `SOURCEID = view_device_v2.device_pk AND IMPORTSOURCE = 'Device42'` → `NODEID` | N:1 |

현재 적재 대상인 `physical` 레코드만 사용한다. 실제 IP·MAC·포트는 별도
`cluster` 레코드에 있으나 두 레코드를 잇는 FK가 없어 추적할 수 없다. 이름
접미사에 의존한 추정 조인은 하지 않는다. ISSUE-2 참조.

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
| 물리 레코드만 | `d.type = 'physical'` | 현재 DEPLOYEDASSET 적재 대상 레코드를 유지한다 |
| 네트워크 장비만 | `d.network_device = true` | ASSETCLASS=NETDEVICE 대상만 적재한다 |
| cluster 제외 | `d.type <> 'cluster'` | 연결 FK가 없으므로 이름 기반 추정 조인을 하지 않는다 |
| 부모 존재 | 교차키 조회 결과가 있는 것만 | 부모가 없으면 적재할 수 없다 |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N |  |  |  |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N |  |  |  |
| DESCRIPTION1 | 설명 | ALN(128) | Y |  |  |  |
| FIRMWAREVERSION | 펌웨어 버전 | ALN(128) | Y |  |  |  |
| NETMACADDR | MAC 주소 | ALN(17) | Y | 원천없음 | – | 실제 값은 분리된 `cluster`의 `view_netport_v1.hwaddress`에 있으나 연결 FK가 없어 추적할 수 없다 |
| NETSOURCEID1 | 네트워크 소스 ID | ALN(128) | Y |  |  |  |
| NETWORKADDRESS | 네트워크 주소 | ALN(39) | Y | 원천없음 | – | 실제 IP는 분리된 `cluster`의 `view_ipaddress_v1`에 있으나 연결 FK가 없어 추적할 수 없다 |
| NODEID | 노드 ID | BIGINT(19) | N |  |  |  |
| OSVERSION | 운영 체제 버전 | ALN(128) | Y |  |  |  |
| RAMSIZE | RAM 크기 | DECIMAL(10,2) | Y |  |  |  |
| RAMUNIT | RAM 단위 | ALN(16) | Y |  |  |  |
| VRAMSIZE | RAM 크기 | ALN(32) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

3번 조건이 반영된, Device42 에서 원천을 끌어오는 SELECT 를 둔다.

## 6. 미결

- ISSUE-2 — `physical`만 사용하고 FK가 없는 `cluster`의 IP·MAC·포트는 추정 조인하지 않는다.
