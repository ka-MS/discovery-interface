# DPAMOSVARIANT

운영체제 변환 변형

> Target: MAXIMO.DPAMOSVARIANT · 구현: [DpamOsVariantImport](../../../../src/main/java/com/itmsg/device42/pipeline/d42maximo/conversion/os/DpamOsVariantImport.java) · [OperatingSystemNamesQuery](../../../../src/main/java/com/itmsg/device42/source/device42/conversion/os/OperatingSystemNamesQuery.java) · [DpamOsVariantWriter](../../../../src/main/java/com/itmsg/device42/target/maximo/conversion/DpamOsVariantWriter.java)
> 관측 2026-08-28 · Device42 192.168.1.35 · 192.168.2.68 / Maximo BLUDB
> 재조회 `../../exploration-queries/maximo/dpa-view-conversion-requirements.sql`

> SQL의 LIMIT/OFFSET은 예시 페이지 값이다. 본체·관계 조회는 Source, 타겟 식별자·값 생성은 Pipeline Mapper가 소유한다.

## 1. 관계

- 부모: 없음. 노드와 무관한 전역 사전이다
- 카디널리티: 원시 문자열 1건 = 행 1건
- 선행: `DPAMOS` 적재. 이 테이블의 `OSNAME` 이 그 대상을 가리킨다
- MERGE 키: `OSVARIANT` (유일 인덱스)

**UI 뷰가 실제로 조인하는 테이블이다.** 값이 없으면 자식 행이 화면에서
사라진다. 짝이 되는 대상 테이블은 `dpamos.md` 참조.

`DPACOS` 뷰가 제조사와 함께 두 사전을 동시에 요구한다.

```sql
from dpaos, dpammanuvariant, dpamosvariant
where dpaos.manufacturer = dpammanuvariant.manufacturervar
  and dpaos.name         = dpamosvariant.osvariant
```

## 2. 테이블 매핑

| Source | Target | 카디널리티 |
| --- | --- | --- |
| `view_deviceos_v1.os_name` | MAXIMO.DPAMOSVARIANT | N:1 |

`OsQuery` 와 같은 원천·같은 필터를 쓴다. 자식이
`defaultUnknown(os_name)`으로 비어 있는 OS명을 `UNKNOWN`으로 기록한다. 그러나 현재 변환 조회는 비어 있지 않은 OS명만 수집하며 `UNKNOWN` 상수를 추가하지 않는다. 원천에 그 이름이 없고 Maximo에도 등록되지 않았다면 해당 자식의 UI 변환 조인이 성립하지 않을 수 있다.

기존 23행은 `Windows 98`, `Windows 2000`, `HP-UX` 같은 구세대 목록이며 Device42
가 수집하는 이름과 겹치는 값이 하나도 없다.

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
| COMPUTER 대상 | `OsQuery` 와 동일한 `MaximoSourcePolicy.ASSET_COMPUTER` | 자식이 기록할 값만 등록한다 |
| 빈 값 제외 | `name IS NOT NULL AND name <> ''` | 이름 컬럼이 NOT NULL 이다 |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| DPAMOSVARIANTID | 운영 체제 변형 ID | BIGINT(19) | N | 채번 | – | `NEXT VALUE FOR MAXIMO.DPAMOSVARIANTSEQ`(START 24). NOT MATCHED 시에만 발번 |
| OSNAME | 대상 운영 체제 | ALN(256) | N | 직접 | 대상 테이블의 정규명 | 정규화 없이 쓰면 원시값과 같다 |
| OSVARIANT | 운영 체제 변형 | ALN(256) | N | 직접 | `view_deviceos_v1.os_name` | 유일 인덱스. MERGE 키. **뷰가 조인하는 컬럼** |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

정규화 없이 적재하면 `OSNAME` 과 `OSVARIANT` 가 같은 값이 된다. 그러면 변환
계층이 실질적으로 동작하지 않는다. 기존 23행도 전건 그 상태이며
`VALIDATED` 가 `0` 이다. 정규화 규칙은 `../../open-issues.md` ISSUE-6 참조.

## 5. 조회 쿼리

대상 테이블과 같은 쿼리를 쓴다.

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
LIMIT 1000 OFFSET 0
```

관측 `.35` 19종, `.68` 14종이다.

## 6. 적재 쿼리

```sql
MERGE INTO MAXIMO.DPAMOSVARIANT AS target
USING (
    VALUES (?, ?)
) AS source (
    OSNAME,
    OSVARIANT
)
ON target.OSVARIANT = source.OSVARIANT
WHEN NOT MATCHED THEN
    INSERT (
        DPAMOSVARIANTID,
        OSNAME,
        OSVARIANT
    )
    VALUES (
        NEXT VALUE FOR MAXIMO.DPAMOSVARIANTSEQ,
        source.OSNAME,
        source.OSVARIANT
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
