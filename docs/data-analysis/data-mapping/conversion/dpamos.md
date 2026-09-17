# DPAMOS

운영체제 변환 대상

> Target: MAXIMO.DPAMOS · 구현: [DpamOsImport](../../../../src/main/java/com/itmsg/device42/integration/d42maximo/conversion/os/DpamOsImport.java) · [DpamOsQuery](../../../../src/main/java/com/itmsg/device42/integration/d42maximo/conversion/os/DpamOsQuery.java) · [DpamOsWriter](../../../../src/main/java/com/itmsg/device42/maximo/conversion/DpamOsWriter.java)
> 관측 2026-08-28 · Device42 192.168.1.35 · 192.168.2.68 / Maximo BLUDB
> 재조회 `../../exploration-queries/maximo/dpa-view-conversion-requirements.sql`

## 1. 관계

- 부모: 없음. 노드와 무관한 전역 사전이다
- 카디널리티: 이름 1건 = 행 1건
- 선행: 없음. Device42 를 직접 조회한다. `ConversionIntegrationJob`의 명시적 순서 3번.
- MERGE 키: `OSNAME` (유일 인덱스)

정규명 목록이다. 뷰가 직접 조인하는 것은 짝이 되는 `DPAMOSVARIANT` 이며
(`dpamosvariant.md` 참조), 이 테이블은 변형이 가리키는 대상을 보관한다.

`DPACOS` 뷰가 제조사와 함께 두 사전을 동시에 요구한다.

```sql
from dpaos, dpammanuvariant, dpamosvariant
where dpaos.manufacturer = dpammanuvariant.manufacturervar
  and dpaos.name         = dpamosvariant.osvariant
```

## 2. 테이블 매핑

| Source | Target | 카디널리티 |
| --- | --- | --- |
| `view_deviceos_v1.os_name` | MAXIMO.DPAMOS | N:1 (같은 이름이 여러 곳에 나타난다) |

`OsQuery` 와 같은 원천·같은 필터를 쓴다. 자식이
`defaultUnknown(os_name)` 으로 기록하므로 상수 `UNKNOWN` 도 함께 넣는다.

기존 23행은 `Windows 98`, `Windows 2000`, `HP-UX` 같은 구세대 목록이며 Device42
가 수집하는 이름과 겹치는 값이 하나도 없다.

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
| COMPUTER 대상 | `OsQuery` 와 동일한 `DEVICE_FILTER` | 자식이 기록할 값만 등록한다 |
| 빈 값 제외 | `name IS NOT NULL AND name <> ''` | 이름 컬럼이 NOT NULL 이다 |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| OSID | OS ID | BIGINT(19) | N | 채번 | – | `NEXT VALUE FOR MAXIMO.DPAMOSSEQ`(START 24). NOT MATCHED 시에만 발번 |
| OSNAME | 대상 운영 체제 | ALN(256) | N | 직접 | `view_deviceos_v1.os_name` | 유일 인덱스. MERGE 키 |
| VALIDATED | 검토됨 | YORN(1) | N | 상수 | – | `0`. 기존 23행도 전건 `0` |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

```sql
SELECT DISTINCT o.os_name AS name
FROM view_deviceos_v1 o
JOIN view_device_v2 d ON d.device_pk = o.device_fk
WHERE
      d.type IN ('virtual', 'physical')
      AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
      AND (d.network_device = false OR d.network_device IS NULL)
      AND (d.physicalsubtype IS NULL OR d.physicalsubtype NOT IN ('Network Printer', 'PDU'))
  AND o.os_name IS NOT NULL
  AND o.os_name <> ''
ORDER BY name
```

관측 `.35` 19종, `.68` 14종이다.

## 6. 적재 쿼리

갱신할 값이 없어 `MATCHED` 분기를 두지 않는다.

```sql
MERGE INTO MAXIMO.DPAMOS AS target
USING (
    VALUES (?)
) AS source (
    OSNAME
)
ON target.OSNAME = source.OSNAME
WHEN NOT MATCHED THEN
    INSERT (
        OSID,
        OSNAME,
        VALIDATED
    )
    VALUES (
        NEXT VALUE FOR MAXIMO.DPAMOSSEQ,
        source.OSNAME,
        0
    )
```

## 7. 관측

19종이 59행을 가리고 있다. 상위 몇 건은 아래와 같다.

| 값 | 행 |
| --- | --- |
| `Red Hat Enterprise Linux 8(64비트)` | 15 |
| `Microsoft Windows Server 2012(64비트)` | 9 |
| `Ubuntu Linux(64비트)` | 7 |
| `CentOS 7(64비트)` | 5 |
| `VMware ESXi` | 4 |

전체 목록은 재조회 쿼리의 `os-missing` 블록으로 얻는다.

## 8. 미결

- **정규화가 가장 필요한 도메인이다.** 같은 OS 가 표기만 달라 여러 종으로
  흩어져 있다. `CentOS 7(64비트)`·`CentOS Linux 7`,
  `Ubuntu`·`Ubuntu Linux(64비트)`·`Canonical Ubuntu 24.04 LTS` 가 그렇다.
- 최장값(151자)은 `cpe:` 문자열까지 들어 있는 수집 잔재다. 정규명으로 등록할
  값이 아니다. 제외 규칙을 둘지, 적재 단계에서 `DPAOS.NAME` 을 정리할지 정해야 한다.
- `(64비트)` 접미는 아키텍처 정보이며 `view_deviceos_v1.os_arch_name` 에도 따로
  있다. 이름에서 떼어낼지 정해야 한다.
