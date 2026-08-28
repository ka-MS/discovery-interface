# DPAMMANUVARIANT

제조업체 변환 변형

> Target: MAXIMO.DPAMMANUVARIANT · 구현: DpamManuVariantIntegrate.java
> 관측 2026-08-28 · Device42 192.168.1.35 · 192.168.2.68 / Maximo BLUDB
> 재조회 `../../exploration-queries/maximo/dpa-view-conversion-requirements.sql`

## 1. 관계

- 부모: 없음. 노드와 무관한 전역 사전이다
- 카디널리티: 원시 문자열 1건 = 행 1건
- 선행: `DPAMMANUFACTURER` 적재. 이 테이블의 `MANUFACTURERNAME` 이 그 대상을 가리킨다
- MERGE 키: `MANUFACTURERVAR` (유일 인덱스)

**UI 뷰가 실제로 조인하는 테이블이다.** 값이 없으면 자식 행이 화면에서
사라진다. 짝이 되는 대상 테이블은 `dpammanufacturer.md` 참조.

**모든** `DPA*` UI 뷰가 이 도메인을 요구한다. 미등록 제조사 하나가 자식 테이블
전 도메인의 행을 동시에 가린다.

```sql
-- NETDEVICE, COMPUTERSYSTEM, NETPRINTER (부모 값으로 조인)
... deployedasset.manufacturer = dpammanuvariant.manufacturervar

-- DPACCPU, DPACOS, DPACNETADAPTER 등 (자식 값으로 조인)
... <자식>.manufacturer = dpammanuvariant.manufacturervar
```

## 2. 테이블 매핑

| Source | Target | 카디널리티 |
| --- | --- | --- |
| `view_vendor_v1.name` | MAXIMO.DPAMMANUVARIANT | N:1 |

자식이 `MANUFACTURER` 에 쓰는 값의 원천은 다섯 갈래다. 각 갈래의 `vendor_fk` 로
`view_vendor_v1.name` 에 도달한다.

| 경로 | 도달하는 자식 컬럼 |
| --- | --- |
| `view_device_v2.hardware_fk` → `view_hardware_v1.vendor_fk` | `DEPLOYEDASSET.MANUFACTURER` |
| `view_part_v1.partmodel_fk` → `view_partmodel_v1.vendor_fk` | `DPACPU`, `DPADISK`, `DPAMEDIAADAPTER` |
| `view_deviceos_v1.os_fk` → `view_os_v1.vendor_fk` | `DPAOS.MANUFACTURER` |
| `view_netport_v1.vendor_fk` | `DPANETADAPTER.MANUFACTURER` |
| `view_softwareinuse_v1.software_fk` → `view_software_v1.vendor_fk` | `TLOAMSOFTWARE.MANUFACTURER` |

자식이 `defaultUnknown(vendor_name)` 으로 기록하므로 상수 `UNKNOWN` 도 함께 넣는다.

**소프트웨어 경로를 빼면 안 된다.** `DPACSOFTWARE` 뷰는 제조사를
`TLOAMSOFTWARE.MANUFACTURER` 에서 가져와 조인한다. 이 경로가 빠지면 소프트웨어
카탈로그를 적재해도 UI 에서 다시 탈락한다. 실측으로 `.35` 11→15종,
`.68` 17→55종 차이다.

Maximo 쪽에 다른 벤더 사전은 없다. `COMPANIES`(109행), `INVVENDOR`(323행)는
구매 도메인이고 `DPA*` UI 뷰 중 이 둘을 참조하는 뷰가 없다.

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
| 부모 범위 | `type IN ('virtual','physical')` + `virtualsubtype_id <> 15` + `physicalsubtype <> 'PDU'` | 하드웨어 경로는 부모 범위다. `Network Printer` 를 빼면 `Samsung` 이 누락된다 |
| COMPUTER 범위 | 위 + `network_device` 거짓 + `physicalsubtype <> 'Network Printer'` | 파트·OS·포트·소프트웨어 경로는 COMPUTER 자식 범위다 |
| 빈 값 제외 | `name IS NOT NULL AND name <> ''` | 이름 컬럼이 NOT NULL 이다 |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| DPAMMANUVARIANTID | 제조업체 변형 ID | BIGINT(19) | N | 채번 | – | `NEXT VALUE FOR MAXIMO.DPAMMANUVARIANTSEQ`(START 382). NOT MATCHED 시에만 발번 |
| MANUFACTURERNAME | 대상 제조업체 | ALN(128) | N | 직접 | 대상 테이블의 정규명 | 정규화 없이 쓰면 원시값과 같다 |
| MANUFACTURERVAR | 제조업체 변형 | ALN(128) | N | 직접 | `view_vendor_v1.name` | 유일 인덱스. MERGE 키. **뷰가 조인하는 컬럼** |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

정규화 없이 적재하면 `MANUFACTURERNAME` 과 `MANUFACTURERVAR` 가 같은 값이 된다. 그러면 변환
계층이 실질적으로 동작하지 않는다. 기존 315행도 전건 그 상태이며
`VALIDATED` 가 `0` 이다. 정규화 규칙은 `../../open-issues.md` ISSUE-6 참조.

## 5. 조회 쿼리

대상 테이블과 같은 쿼리를 쓴다.

```sql
WITH target AS (
    SELECT d.device_pk, d.hardware_fk
    FROM view_device_v2 d
    WHERE
      d.type IN ('virtual', 'physical')
      AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
      AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'PDU')
),
computer AS (
    SELECT d.device_pk
    FROM view_device_v2 d
    WHERE
      d.type IN ('virtual', 'physical')
      AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
      AND (d.network_device = false OR d.network_device IS NULL)
      AND (d.physicalsubtype IS NULL OR d.physicalsubtype NOT IN ('Network Printer', 'PDU'))
),
names AS (
    SELECT v.name FROM target t
    JOIN view_hardware_v1 h ON h.hardware_pk = t.hardware_fk
    JOIN view_vendor_v1 v ON v.vendor_pk = h.vendor_fk
    UNION
    SELECT v.name FROM view_part_v1 p
    JOIN computer c ON c.device_pk = p.device_fk
    JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
    JOIN view_vendor_v1 v ON v.vendor_pk = pm.vendor_fk
    UNION
    SELECT v.name FROM view_deviceos_v1 o
    JOIN computer c ON c.device_pk = o.device_fk
    JOIN view_os_v1 s ON s.os_pk = o.os_fk
    JOIN view_vendor_v1 v ON v.vendor_pk = s.vendor_fk
    UNION
    SELECT v.name FROM view_netport_v1 n
    JOIN computer c ON c.device_pk = n.device_fk
    JOIN view_vendor_v1 v ON v.vendor_pk = n.vendor_fk
    UNION
    SELECT v.name FROM view_softwareinuse_v1 u
    JOIN computer c ON c.device_pk = u.device_fk
    JOIN view_software_v1 sw ON sw.software_pk = u.software_fk
    JOIN view_vendor_v1 v ON v.vendor_pk = sw.vendor_fk
    UNION
    SELECT 'UNKNOWN'
),
filtered AS (
    SELECT name FROM names WHERE name IS NOT NULL AND name <> ''
)
SELECT name FROM filtered ORDER BY name
```

관측 `.35` 15종, `.68` 55종이다.

## 6. 적재 쿼리

```sql
MERGE INTO MAXIMO.DPAMMANUVARIANT AS target
USING (
    VALUES (?, ?)
) AS source (
    MANUFACTURERNAME,
    MANUFACTURERVAR
)
ON target.MANUFACTURERVAR = source.MANUFACTURERVAR
WHEN NOT MATCHED THEN
    INSERT (
        DPAMMANUVARIANTID,
        MANUFACTURERNAME,
        MANUFACTURERVAR
    )
    VALUES (
        NEXT VALUE FOR MAXIMO.DPAMMANUVARIANTSEQ,
        source.MANUFACTURERNAME,
        source.MANUFACTURERVAR
    )
```

## 7. 관측

관측 시점 미등록 5종이며 8행을 가리고 있다.

| 값 | 나타나는 테이블 | 가리는 행 | 비고 |
| --- | --- | --- | --- |
| `Cisco` | `DEPLOYEDASSET` | 2 | Device42 적재분. NETDEVICE 2건 |
| `Canonical` | `DPAOS` | 2 | Device42 적재분 |
| `GIGABYTE` | `DEPLOYEDASSET` | 1 | Device42 적재분 |
| `CentOS` | `DPAOS` | 1 | Device42 적재분 |
| `Sun_microsystems` | `DEPLOYEDASSET` | 2 | 기존 수집분. Device42 원천에 없어 등록되지 않는다 |

`UNKNOWN`·`Dell`·`Samsung` 은 이미 등록되어 있다.

## 8. 미결

- 별칭 통합 여부. Device42 벤더 목록 자체가 정규화되어 있지 않다. `IBM` 과
  `IBM Corporation`, `Microsoft` 와 `Microsoft Corp.` 가 각각 별개 행이다.
- `Sun_microsystems` 는 기존 수집분 값이며 Device42 원천에 없다. 이 매핑은
  Device42 만 조회하므로 등록되지 않는다. 기존 수집분 2행은 계속 미표시로 남는다.
