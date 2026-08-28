# DPAMADAPTER

어댑터 변환 대상

> Target: MAXIMO.DPAMADAPTER · 구현: DpamAdapterIntegrate.java
> 관측 2026-08-28 · Device42 192.168.1.35 · 192.168.2.68 / Maximo BLUDB
> 재조회 `../../exploration-queries/maximo/dpa-view-conversion-requirements.sql`

## 1. 관계

- 부모: 없음. 노드와 무관한 전역 사전이다
- 카디널리티: 이름 1건 = 행 1건
- 선행: 없음. Device42 를 직접 조회한다. `conversion` 잡 `@Order(7)`
- MERGE 키: `ADAPTERNAME` (유일 인덱스)

정규명 목록이다. 뷰가 직접 조인하는 것은 짝이 되는 `DPAMADPTVARIANT` 이며
(`dpamadptvariant.md` 참조), 이 테이블은 변형이 가리키는 대상을 보관한다.

어댑터 계열 뷰 넷이 요구한다. `DPACNETADAPTER`, `DPACMEDIAADAPTER`,
`DPACCOMMDEVICE`, `DPACNETDEVCARD` 다.

```sql
from dpanetadapter, dpammanuvariant, dpamadptvariant
where dpanetadapter.manufacturer = dpammanuvariant.manufacturervar
  and dpanetadapter.makemodel    = dpamadptvariant.adaptervariant
```

## 2. 테이블 매핑

| Source | Target | 카디널리티 |
| --- | --- | --- |
| `view_partmodel_v1.name` (`type_name = 'GPU'`) + 상수 `UNKNOWN` | MAXIMO.DPAMADAPTER | N:1 (같은 이름이 여러 곳에 나타난다) |

두 자식의 `MAKEMODEL` 성격이 다르다.

| 자식 | 기록되는 값 | Device42 원천 |
| --- | --- | --- |
| `DPAMEDIAADAPTER` | `defaultUnknown(view_partmodel_v1.name)` | GPU 파트 모델명 |
| `DPANETADAPTER` | **상수 `UNKNOWN`** | **대응 컬럼 없음** |

`DPANETADAPTER.MAKEMODEL` 은 `view_netport_v1` 에 어댑터 모델 컬럼이 없어 매핑이
상수로 정한 값이다. Device42 에서 조회할 수 없으므로 GPU 모델명에 상수
`UNKNOWN` 을 더해 만든다. 근거는 `../asset/dpanetadapter.md` 참조.

기존 59행은 `3Com 3C920 ...` 같은 구세대 NIC 모델명이다.

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
| GPU 파트만 | `pm.type_name = 'GPU'` | `view_part_v1` 은 여러 파트 종류를 한 테이블에 담는다 |
| COMPUTER 대상 | `DpaMediaAdapterIntegrate` 와 동일한 `DEVICE_FILTER` | 자식이 기록할 값만 등록한다 |
| 상수 추가 | `UNION SELECT 'UNKNOWN'` | `DPANETADAPTER` 가 기록하는 값 |
| 빈 값 제외 | `name IS NOT NULL AND name <> ''` | 이름 컬럼이 NOT NULL 이다 |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| ADAPTERID | 어댑터 ID | BIGINT(19) | N | 채번 | – | `NEXT VALUE FOR MAXIMO.DPAMADAPTERSEQ`(START 60). NOT MATCHED 시에만 발번 |
| ADAPTERNAME | 대상 어댑터 | ALN(128) | N | 직접 | `view_partmodel_v1.name` (`type_name = 'GPU'`) + 상수 `UNKNOWN` | 유일 인덱스. MERGE 키 |
| VALIDATED | 검토됨 | YORN(1) | N | 상수 | – | `0`. 기존 59행도 전건 `0` |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

```sql
WITH gpu AS (
    SELECT DISTINCT pm.name
    FROM view_part_v1 p
    JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
    JOIN view_device_v2 d ON d.device_pk = p.device_fk
    WHERE pm.type_name = 'GPU'
      AND
      d.type IN ('virtual', 'physical')
      AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
      AND (d.network_device = false OR d.network_device IS NULL)
      AND (d.physicalsubtype IS NULL OR d.physicalsubtype NOT IN ('Network Printer', 'PDU'))
      AND pm.name IS NOT NULL
      AND pm.name <> ''
)
SELECT name FROM gpu
UNION
SELECT 'UNKNOWN'
ORDER BY name
```

관측 `.35` 2종, `.68` 6종이다.

## 6. 적재 쿼리

갱신할 값이 없어 `MATCHED` 분기를 두지 않는다.

```sql
MERGE INTO MAXIMO.DPAMADAPTER AS target
USING (
    VALUES (?)
) AS source (
    ADAPTERNAME
)
ON target.ADAPTERNAME = source.ADAPTERNAME
WHEN NOT MATCHED THEN
    INSERT (
        ADAPTERID,
        ADAPTERNAME,
        VALIDATED
    )
    VALUES (
        NEXT VALUE FOR MAXIMO.DPAMADAPTERSEQ,
        source.ADAPTERNAME,
        0
    )
```

## 7. 관측

2종이 145행을 가리고 있다. 그중 1종이 144행을 차지한다.

| 값 | 출처 | 행 |
| --- | --- | --- |
| `UNKNOWN` | `DPANETADAPTER` | 144 |
| `VMware SVGA 3D` | `DPAMEDIAADAPTER` | 1 |

## 8. 미결

- `UNKNOWN` 을 사전에 넣는 것이 옳은지 판단이 필요하다. 값이 아니라 값 없음을
  뜻하므로 변환 데이터의 성격과 맞지 않는다. `DPANETADAPTER.MAKEMODEL` 에 다른
  원천을 찾아 채우는 것이 대안이다.
- `MAKEMODEL` 은 `MAXATTRIBUTE.DEFAULTVALUE` 가 `UNKNOWN` 인 NOT NULL 컬럼이라
  값을 비울 수는 없다.
