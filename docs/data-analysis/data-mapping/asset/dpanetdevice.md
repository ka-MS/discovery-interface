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
| `view_netport_v1` | (연결) | `second_device_fk` = 물리 `device_pk`, `device_fk` = cluster `device_pk` | N:1 |
| `view_ipaddress_v2` | (보강) | cluster `device_pk` = `ANY(device_fks)` | N:1 |

레코드 분리 구조와 값 귀속 규칙은 `../../open-issues.md` ISSUE-2 참조.

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
| NETMACADDR | MAC 주소 | ALN(17) | Y | 변환 | `view_netport_v1.hwaddress` | cluster 대응 후 그 스위치 포트들의 최솟값. 베이스 MAC 은 `second_device_fk` 가 비어 자동 제외된다. 5절 절차 3 |
| NETSOURCEID1 | 네트워크 소스 ID | ALN(128) | Y | 원천없음 | – | 기존 수집분도 0/34 |
| NETWORKADDRESS | 네트워크 주소 | ALN(39) | Y | 변환 | `view_ipaddress_v2.ip_address` | cluster 대응 후 관리 IP. 관측 2/2 · 2/2, cluster 당 IP 1개. 다중 멤버 시 여러 행이 같은 값을 갖는다. 기존 수집분 29/34 |
| NODEID | 노드 ID | BIGINT(19) | N | 직접 | 물리 `view_device_v2.device_pk` | 부모 DEPLOYEDASSET와 동일한 ID를 직접 사용하며 MERGE 키로 삼는다 |
| OSVERSION | 운영 체제 버전 | ALN(128) | Y | 직접 | `view_device_v2.os_version` | 물리 레코드 값. 예: `Gibraltar 16.12.4`, `12.2(25)SEB4`. 관측 2/2 · 2/2. 기존 수집분은 0/34 라 새로 채우는 값이다 |
| RAMSIZE | RAM 크기 | DECIMAL(10,2) | Y | 원천없음 | – | `view_device_v2.ram` 이 스위치 레코드에서 전건 비어 있다. 기존 수집분은 전건 `0.00` 자리표시 |
| RAMUNIT | RAM 단위 | ALN(16) | Y | 원천없음 | – | 기존 수집분은 전건 `KB` 자리표시 |
| VRAMSIZE | RAM 크기 | ALN(32) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

```sql
WITH target AS (
    SELECT device_pk, os_version
    FROM view_device_v2
    WHERE network_device = true
      AND type = 'physical'
),
link AS (
    SELECT second_device_fk AS physical_pk,
           device_fk        AS cluster_pk,
           MIN(hwaddress)   AS mac
    FROM view_netport_v1
    WHERE second_device_fk IS NOT NULL
      AND hwaddress IS NOT NULL
      AND hwaddress <> ''
    GROUP BY second_device_fk, device_fk
)
SELECT t.device_pk,
       t.os_version,
       l.mac,
       (SELECT MIN(ip_address)
          FROM view_ipaddress_v2
         WHERE l.cluster_pk = ANY(device_fks)) AS mgmt_ip
FROM target t
LEFT JOIN link l ON l.physical_pk = t.device_pk
ORDER BY t.device_pk
```

### 전제

- MAC 전용 뷰가 없다 → MAC 은 `view_netport_v1.hwaddress` 에만 있다.
- 네트워크 장비는 두 레코드로 쪼개진다 → `physical` 에 시리얼·OS, `cluster` 에 포트·MAC·IP.
- 부모는 `physical` 만 적재한다 → 적재 대상에 네트워크 정보가 없다.
- 장비 레벨 FK 는 전부 비어 있다 → 유일한 연결은 포트의 `second_device_fk`(cluster 포트 → 물리 스위치).

### 절차

1. **대상** — `view_device_v2` 에서 `network_device = true AND type = 'physical'` → `device_pk`, `os_version`
2. **cluster 역추적** — `view_netport_v1` 에서 `second_device_fk = device_pk` 인 포트들 → 그 `device_fk` 가 cluster
3. **MAC** — 2번 포트들의 `hwaddress` 최솟값. 베이스 MAC 은 `second_device_fk` 가 비어 자동 제외되므로 그 스위치 고유값만 남는다
4. **IP** — `Vlan1` 포트도 `second_device_fk` 가 비어 포트 경유가 안 된다. cluster `device_pk` 가 `device_fks` 에 든 행을 직접 조회해 최솟값
5. **부모 키** — 물리 `device_pk` 를 `NODEID` 로 직접 사용한다

양쪽 서버 실행 결과가 일치한다. `JAE24461ECG` 는 `549fc6badb81` · `192.168.2.3`,
`CAT1040RGWU` 는 `0019aa435281` · `192.168.2.2` 다.

## 6. 미결

- 다중 멤버 스택을 실측하지 못했다. 관측 시점에 cluster 당 물리 멤버가 1개뿐이다.
  멤버가 둘 이상인 스택이 생기면 재확인한다. ISSUE-2 참조.
