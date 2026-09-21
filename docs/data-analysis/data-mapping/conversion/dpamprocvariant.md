# DPAMPROCVARIANT

프로세서 변환 변형

> Target: MAXIMO.DPAMPROCVARIANT · 구현: [DpamProcVariantImport](../../../../src/main/java/com/itmsg/device42/pipeline/d42maximo/conversion/processor/DpamProcVariantImport.java) · [ProcessorModelsQuery](../../../../src/main/java/com/itmsg/device42/source/device42/conversion/processor/ProcessorModelsQuery.java) · [DpamProcVariantWriter](../../../../src/main/java/com/itmsg/device42/target/maximo/conversion/DpamProcVariantWriter.java)
> 관측 2026-08-28 · Device42 192.168.1.35 · 192.168.2.68 / Maximo BLUDB
> 재조회 `../../exploration-queries/maximo/dpa-view-conversion-requirements.sql`

> SQL의 LIMIT/OFFSET은 예시 페이지 값이다. 본체·관계 조회는 Source, 타겟 식별자·값 생성은 Pipeline Mapper가 소유한다.

## 1. 관계

- 부모: 없음. 노드와 무관한 전역 사전이다
- 카디널리티: 원시 문자열 1건 = 행 1건
- 선행: `DPAMPROCESSOR` 적재. 이 테이블의 `PROCESSORNAME` 이 그 대상을 가리킨다
- MERGE 키: `PROCESSORVAR` (유일 인덱스)

**UI 뷰가 실제로 조인하는 테이블이다.** 값이 없으면 자식 행이 화면에서
사라진다. 짝이 되는 대상 테이블은 `dpamprocessor.md` 참조.

`DPACCPU` 뷰가 요구한다.

```sql
from dpacpu, dpammanuvariant, dpamprocvariant
where dpacpu.manufacturer = dpammanuvariant.manufacturervar
  and dpacpu.makemodel    = dpamprocvariant.processorvar
```

## 2. 테이블 매핑

| Source | Target | 카디널리티 |
| --- | --- | --- |
| `view_partmodel_v1.name` (`type_name = 'CPU'`) | MAXIMO.DPAMPROCVARIANT | N:1 |

`CpuQuery` 와 같은 원천·같은 필터를 쓴다. 자식이
`defaultUnknown(model_name)`으로 비어 있는 모델명을 `UNKNOWN`으로 기록한다. 그러나 현재 변환 조회는 비어 있지 않은 CPU 모델명만 수집하며 `UNKNOWN` 상수를 추가하지 않는다. 원천에 그 이름이 없고 Maximo에도 등록되지 않았다면 해당 자식의 UI 변환 조인이 성립하지 않을 수 있다.

기존 13행은 Device42 가 수집하는 모델명과 겹치는 값이 없다.

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
| CPU 파트만 | `pm.type_name = 'CPU'` | `view_part_v1` 은 여러 파트 종류를 한 테이블에 담는다 |
| COMPUTER 대상 | `CpuQuery` 와 동일한 `MaximoSourcePolicy.ASSET_COMPUTER` | 자식이 기록할 값만 등록한다 |
| 빈 값 제외 | `name IS NOT NULL AND name <> ''` | 이름 컬럼이 NOT NULL 이다 |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| DPAMPROCVARIANTID | 프로세서 변형 ID | BIGINT(19) | N | 채번 | – | `NEXT VALUE FOR MAXIMO.DPAMPROCVARIANTSEQ`(START 14). NOT MATCHED 시에만 발번 |
| PROCESSORNAME | 대상 프로세서 | ALN(128) | N | 직접 | 대상 테이블의 정규명 | 정규화 없이 쓰면 원시값과 같다 |
| PROCESSORVAR | 프로세서 변형 | ALN(128) | N | 직접 | `view_partmodel_v1.name` (`type_name = 'CPU'`) | 유일 인덱스. MERGE 키. **뷰가 조인하는 컬럼** |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

정규화 없이 적재하면 `PROCESSORNAME` 과 `PROCESSORVAR` 가 같은 값이 된다. 그러면 변환
계층이 실질적으로 동작하지 않는다. 기존 13행도 전건 그 상태이며
`VALIDATED` 가 `0` 이다. 정규화 규칙은 `../../open-issues.md` ISSUE-6 참조.

## 5. 조회 쿼리

대상 테이블과 같은 쿼리를 쓴다.

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

```sql
MERGE INTO MAXIMO.DPAMPROCVARIANT AS target
USING (
    VALUES (?, ?)
) AS source (
    PROCESSORNAME,
    PROCESSORVAR
)
ON target.PROCESSORVAR = source.PROCESSORVAR
WHEN NOT MATCHED THEN
    INSERT (
        DPAMPROCVARIANTID,
        PROCESSORNAME,
        PROCESSORVAR
    )
    VALUES (
        NEXT VALUE FOR MAXIMO.DPAMPROCVARIANTSEQ,
        source.PROCESSORNAME,
        source.PROCESSORVAR
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
