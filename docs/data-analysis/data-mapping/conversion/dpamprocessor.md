# DPAMPROCESSOR

프로세서 변환 대상

> Target: MAXIMO.DPAMPROCESSOR · 구현: [DpamProcessorImport](../../../../src/main/java/com/itmsg/device42/pipeline/d42maximo/conversion/processor/DpamProcessorImport.java) · [ProcessorModelsQuery](../../../../src/main/java/com/itmsg/device42/source/device42/conversion/processor/ProcessorModelsQuery.java) · [DpamProcessorWriter](../../../../src/main/java/com/itmsg/device42/target/maximo/conversion/DpamProcessorWriter.java)
> 관측 2026-08-28 · Device42 192.168.1.35 · 192.168.2.68 / Maximo BLUDB
> 재조회 `../../exploration-queries/maximo/dpa-view-conversion-requirements.sql`

> SQL의 LIMIT/OFFSET은 예시 페이지 값이다. 본체·관계 조회는 Source, 타겟 식별자·값 생성은 Pipeline Mapper가 소유한다.

## 1. 관계

- 부모: 없음. 노드와 무관한 전역 사전이다
- 카디널리티: 이름 1건 = 행 1건
- 선행: 없음. Device42 를 직접 조회한다. `ConversionIntegrationJob`의 명시적 순서 5번.
- MERGE 키: `PROCESSORNAME` (유일 인덱스)

정규명 목록이다. 뷰가 직접 조인하는 것은 짝이 되는 `DPAMPROCVARIANT` 이며
(`dpamprocvariant.md` 참조), 이 테이블은 변형이 가리키는 대상을 보관한다.

`DPACCPU` 뷰가 요구한다.

```sql
from dpacpu, dpammanuvariant, dpamprocvariant
where dpacpu.manufacturer = dpammanuvariant.manufacturervar
  and dpacpu.makemodel    = dpamprocvariant.processorvar
```

## 2. 테이블 매핑

| Source | Target | 카디널리티 |
| --- | --- | --- |
| `view_partmodel_v1.name` (`type_name = 'CPU'`) | MAXIMO.DPAMPROCESSOR | N:1 (같은 이름이 여러 곳에 나타난다) |

`CpuQuery` 와 같은 원천·같은 필터를 쓴다. 자식이
`defaultUnknown(model_name)` 으로 기록하므로 상수 `UNKNOWN` 도 함께 넣는다.

기존 13행은 Device42 가 수집하는 모델명과 겹치는 값이 없다.

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
| CPU 파트만 | `pm.type_name = 'CPU'` | `view_part_v1` 은 여러 파트 종류를 한 테이블에 담는다 |
| COMPUTER 대상 | `CpuQuery` 와 동일한 `DEVICE_FILTER` | 자식이 기록할 값만 등록한다 |
| 빈 값 제외 | `name IS NOT NULL AND name <> ''` | 이름 컬럼이 NOT NULL 이다 |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| PROCESSORID | 프로세서 ID | BIGINT(19) | N | 채번 | – | `NEXT VALUE FOR MAXIMO.DPAMPROCESSORSEQ`(START 14). NOT MATCHED 시에만 발번 |
| PROCESSORNAME | 대상 프로세서 | ALN(128) | N | 직접 | `view_partmodel_v1.name` (`type_name = 'CPU'`) | 유일 인덱스. MERGE 키 |
| VALIDATED | 검토됨 | YORN(1) | N | 상수 | – | `0`. 기존 13행도 전건 `0` |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

```sql
SELECT DISTINCT pm.name
FROM view_part_v1 p
JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
JOIN view_device_v2 d ON d.device_pk = p.device_fk
WHERE pm.type_name = 'CPU'
  AND
d.type IN ('virtual', 'physical')
AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
AND (d.network_device = false OR d.network_device IS NULL)
AND (d.physicalsubtype IS NULL OR d.physicalsubtype NOT IN ('Network Printer', 'PDU'))

  AND pm.name IS NOT NULL
  AND pm.name <> ''
LIMIT 1000 OFFSET 0
```

관측 `.35` 4종, `.68` 6종이다.

## 6. 적재 쿼리

갱신할 값이 없어 `MATCHED` 분기를 두지 않는다.

```sql
MERGE INTO MAXIMO.DPAMPROCESSOR AS target
USING (
    VALUES (?)
) AS source (
    PROCESSORNAME
)
ON target.PROCESSORNAME = source.PROCESSORNAME
WHEN NOT MATCHED THEN
    INSERT (
        PROCESSORID,
        PROCESSORNAME,
        VALIDATED
    )
    VALUES (
        NEXT VALUE FOR MAXIMO.DPAMPROCESSORSEQ,
        source.PROCESSORNAME,
        0
    )
```

## 7. 관측

4종이 84행을 가리고 있다.

| 값 | 행 |
| --- | --- |
| `Intel(R) Xeon(R) Silver 4214R CPU @ 2.40GHz` | 46 |
| `Intel(R) Xeon(R) Silver 4214R CPU @ 2.40GHz  CPU @ 2.4GHz` | 32 |
| `Intel(R) Xeon(R) Silver 4110 CPU @ 2.10GHz` | 4 |
| `Intel(R) Core(TM) i5-3570K CPU @ 3.40GHz` | 2 |

## 8. 미결

- 1위와 2위가 **같은 CPU 다.** 2위는 모델명 뒤에 `CPU @ 2.4GHz` 가 한 번 더 붙은
  수집 잔재다. 두 값을 정규명 하나로 접으면 78행이 함께 해소된다.
- 이 중복 표기는 `../../close-issues.md` ISSUE-5 의 DPACPU 슬롯 중복과 같은
  원인이다. 적재 단계에서 정리하면 등록도 함께 줄어든다.
