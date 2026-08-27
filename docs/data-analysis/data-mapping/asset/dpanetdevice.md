# DPANETDEVICE

배치된 자산 네트워크 디바이스

> Target: MAXIMO.DPANETDEVICE · ASSETCLASS: NETDEVICE · 구현: DpaNetDeviceIntegrate.java

> 관측 2026-08-27 · Device42 **양쪽 서버** 192.168.2.68 / 192.168.1.35 · Maximo BLUDB
> 원천 건수는 `.68 / .35` 순으로 병기한다.

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : 1 DPANETDEVICE (PK 가 `NODEID` 단독. 관측 34노드/34행)
- 선행: DEPLOYEDASSET

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
| `view_device_v2`(`physical`) | MAXIMO.DPANETDEVICE | – | 1:1 |
| `view_netport_v1` | (연결) | `second_device_fk` = 물리 device_pk, `device_fk` = cluster device_pk | N:1 |
| `view_ipaddress_v1` | (보강) | cluster device_pk 로 관리 IP 조회 | N:1 |
| MAXIMO.DEPLOYEDASSET | (교차키) | `SOURCEID = 물리 device_pk AND IMPORTSOURCE = 'Device42'` → `NODEID` | N:1 |

네트워크 장비는 Device42 에서 두 레코드로 나뉜다. `physical` 레코드가 시리얼과
OS 를 갖고 적재 대상이 되며, `cluster` 레코드가 포트·MAC·관리 IP 를 갖는다.

**두 레코드는 `view_netport_v1.second_device_fk` 로 이어진다.** cluster 소속
포트의 `second_device_fk` 가 물리 레코드를 가리킨다. 양쪽 서버에서 1:1 이며
관측 기준 `.68` 28·26포트, `.35` 28·26포트가 같은 대상을 가리킨다.

```sql
-- cluster ↔ physical 대응
SELECT DISTINCT device_fk AS cluster_pk, second_device_fk AS physical_pk
FROM view_netport_v1 WHERE second_device_fk IS NOT NULL
```

**cluster 는 물리 멤버를 여러 개 가질 수 있다.** Cisco 스택 구성이며 이름의
` - Switch N` 접미가 멤버 번호다. 관측 시점에는 양쪽 서버 모두 cluster 당
멤버가 1개지만 구조상 1:N 이다. 멤버가 늘어나는 경우를 전제로 매핑한다.

`NETMACADDR` 선정 규칙은 미결이다. 단수 컬럼이라 포트 28개 중 하나를 골라야
한다. 후보가 둘이고 의미가 다르다.

| 후보 | 값 | 귀속 | 관점 |
| --- | --- | --- | --- |
| 베이스 MAC (포트 이름 = MAC 인 항목) | `549fc6badb80` | cluster. `second_device_fk` 가 비어 있어 물리 멤버로 연결되지 않는다 | 이 장비에 접속하는 주소. 운영 관점 |
| 멤버 자신의 포트 MAC 최솟값 | `549fc6badb81` | 물리 멤버 | 이 물리 유닛의 MAC. 자산 관점 |

**멤버 여럿이 같은 값을 갖는 것 자체는 문제가 아니다.** 스택은 관리 IP 와
베이스 MAC 을 공유하므로 그것이 물리적 사실이다. 스키마도 막지 않는다.
`DPANETDEVICE` 는 PK 가 `NODEID` 라 멤버마다 자기 행을 갖고, `DEPLOYEDASSET`
의 유니크 인덱스는 `(NODENAME, DOMAINNAME, ASSETCLASS, TLOAMHASH)` 이며
`NODENAME` 은 멤버마다 다르다.

`MAXATTRIBUTE` 의 컬럼 설명은 "네트워크 MAC 주소" 뿐이라 범위를 규정하지
않는다. 기존 수집분 34건은 MAC·IP 중복이 없으나 전부 단독 장비라 정책의
근거가 되지 못한다.

포트와 MAC 은 1:1 이며, 스위치의 물리 포트 MAC 은 빈틈없는 연속 블록이다.
`ITMSG_L2_SW1` 은 26포트가 `0019aa435281`~`0019aa43529a`,
`ITMSG_L3_SW1` 은 28포트가 `549fc6badb81`~`549fc6badb9c` 다.

**베이스 MAC 과 포트 블록의 위치 관계는 일정하지 않다.** `ITMSG_L3_SW1` 은
베이스 `549fc6badb80` 이 포트 블록 바로 아래지만, `ITMSG_L2_SW1` 은 베이스
`0019aa4352c0` 이 포트 블록(`…529a` 까지)보다 위다. 산술로 유도할 수 없으며
베이스는 포트명이 MAC 과 같은 항목으로만 식별된다.

멤버별 OUI 대역이 달라(`549fc6ba…` / `0019aa43…`) 최솟값이 섞이지 않는다.

논리 인터페이스(`Vlan1`, `Loopback Interface`)와 미사용 물리 포트
(`GigabitEthernet0/0`, `Bluetooth0/4`), AWS 가상 인터페이스(`eni-…`)는 MAC 이
없다. 전체 290포트 중 249개만 MAC 을 가지며 그중 248개가 고유하다.

관리 IP 는 cluster 의 `Vlan1` 인터페이스에 붙어 있고 그 포트의
`second_device_fk` 는 비어 있다. 따라서 IP 는 포트 경유가 아니라 위 대응표로
cluster 를 찾아 조회한다. 관측 기준 cluster 당 IP 는 1개다.

스택은 관리 IP 를 공유하므로 멤버가 여럿이면 여러 행이 같은
`NETWORKADDRESS` 를 갖는다. 물리적 사실이며 문제가 아니다. 자산 식별은
`DEPLOYEDASSET.SERIALNUMBER` 로 하며 IP·MAC 은 식별자가 아니다.

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
| 네트워크 장비만 | `d.network_device = true` | ASSETCLASS=NETDEVICE 대상 |
| 물리 레코드만 | `d.type = 'physical'` | 부모 DEPLOYEDASSET 이 적재하는 레코드와 일치시킨다 |

부모의 적재 조건(`type IN ('virtual','physical')`)과 어긋나지 않는다. `cluster`
레코드는 부모가 적재하지 않으므로 여기서도 대상이 아니며, 포트·IP 를 얻는
경로로만 참조한다.

관측 기준 `network_device = true` 는 양쪽 서버 모두 4건이고 그중 적재 대상은
2건이다. 나머지 2건이 `cluster` 다.

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각 |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각 |
| DESCRIPTION1 | 설명 | ALN(128) | Y | 원천없음 | – | 기존 수집분도 0/34. `os_name` 이 후보이나 컬럼 용도 미확인 |
| FIRMWAREVERSION | 펌웨어 버전 | ALN(128) | Y | 원천없음 | – | `bios_version`·`bios_revision`·`bios_fw_revision` 이 스위치 레코드에서 전건 비어 있다. 기존 수집분도 0/34 |
| NETMACADDR | MAC 주소 | ALN(17) | Y | 미결 | `view_netport_v1.hwaddress` | 단수 컬럼이라 포트 28개 중 하나를 골라야 한다. 스택 대표값(베이스 MAC)과 멤버 고유값(자기 포트 MAC 최솟값) 중 선택이며 둘 다 유효하다. 2절 참조 |
| NETSOURCEID1 | 네트워크 소스 ID | ALN(128) | Y | 원천없음 | – | 기존 수집분도 0/34 |
| NETWORKADDRESS | 네트워크 주소 | ALN(39) | Y | 변환 | `view_ipaddress_v1.ip_address` | cluster 대응 후 관리 IP. 관측 2/2 · 2/2, cluster 당 IP 1개. 다중 멤버 시 여러 행이 같은 값을 갖는다. 기존 수집분 29/34 |
| NODEID | 노드 ID | BIGINT(19) | N | 채번 | – | 부모 DEPLOYEDASSET.NODEID. `(SOURCEID, IMPORTSOURCE)` 로 조회 |
| OSVERSION | 운영 체제 버전 | ALN(128) | Y | 직접 | `view_device_v2.os_version` | 물리 레코드 값. 예: `Gibraltar 16.12.4`, `12.2(25)SEB4`. 관측 2/2 · 2/2. 기존 수집분은 0/34 라 새로 채우는 값이다 |
| RAMSIZE | RAM 크기 | DECIMAL(10,2) | Y | 원천없음 | – | `view_device_v2.ram` 이 스위치 레코드에서 전건 비어 있다. 기존 수집분은 전건 `0.00` 자리표시 |
| RAMUNIT | RAM 단위 | ALN(16) | Y | 원천없음 | – | 기존 수집분은 전건 `KB` 자리표시 |
| VRAMSIZE | RAM 크기 | ALN(32) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

```sql
WITH map AS (
    SELECT DISTINCT device_fk AS cluster_pk, second_device_fk AS physical_pk
    FROM view_netport_v1
    WHERE second_device_fk IS NOT NULL
),
base_mac AS (
    SELECT device_fk AS cluster_pk, MIN(hwaddress) AS base_mac
    FROM view_netport_v1
    WHERE hwaddress IS NOT NULL AND hwaddress <> ''
      AND LOWER(port) = LOWER(hwaddress)
    GROUP BY device_fk
),
mgmt_ip AS (
    SELECT device_fk AS cluster_pk, MIN(ip_address) AS mgmt_ip
    FROM view_ipaddress_v1
    GROUP BY device_fk
)
SELECT
    d.device_pk,
    d.name,
    d.os_version,
    b.base_mac,
    m.mgmt_ip
FROM view_device_v2 d
LEFT JOIN map      mp ON mp.physical_pk = d.device_pk
LEFT JOIN base_mac b  ON b.cluster_pk   = mp.cluster_pk
LEFT JOIN mgmt_ip  m  ON m.cluster_pk   = mp.cluster_pk
WHERE d.network_device = true
  AND d.type = 'physical'
ORDER BY d.device_pk
```

양쪽 서버 실행 결과가 일치한다. 시리얼 `JAE24461ECG` 는 베이스 MAC
`549fc6badb80` · 관리 IP `192.168.2.3`, `CAT1040RGWU` 는 `0019aa4352c0` ·
`192.168.2.2` 로 동일하다.

## 6. 미결

- **`NETMACADDR` 선정 규칙 미결.** 스택 대표값(베이스 MAC)과 멤버 고유값(자기
  포트 MAC 최솟값) 중 무엇을 넣을지 정해야 한다. 두 값은 1 차이다.
  멤버 여럿이 같은 값을 갖는 것은 스택 구조상 정상이며 위험하지 않다.
  선택은 의미의 문제다. 기존 수집분의 34건이 베이스인지 첫 포트인지는
  원천이 없어 대조할 수 없다.
- **다중 멤버 스택 미검증.** 관측 시점에 cluster 당 물리 멤버가 1개뿐이라
  1:N 동작을 실측하지 못했다. 멤버가 둘 이상인 스택이 생기면 재확인이 필요하다.
- ISSUE-1 — 동일 스위치가 서버에 따라 다른 `device_pk` 를 갖는다. `.68` 은 13·108,
  `.35` 는 284·283 이고 시리얼(`JAE24461ECG`, `CAT1040RGWU`)은 같다.
- `DESCRIPTION1` 은 원천 후보로 `os_name` 이 있으나 컬럼 용도가 확인되지 않았다.
  기존 수집분은 0/34 다. 채우지 않는다.
- `RAMSIZE`·`RAMUNIT` 은 기존 수집분이 `0.00`·`KB` 로 전건 채워져 있으나 실측값이
  아닌 자리표시다. 같은 방식으로 채울지, NULL 로 둘지 정해야 한다.
