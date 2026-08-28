# DPACPU

배치된 자산 컴퓨터 프로세서

> Target: MAXIMO.DPACPU · ASSETCLASS: COMPUTER · 구현: DpaCpuIntegrate.java

> 관측 2026-08-27 · Device42 **양쪽 서버** 192.168.2.68 / 192.168.1.35 · Maximo BLUDB
> 원천 건수는 서버별로 병기한다. 표기는 `.68 / .35` 순이다.

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : N DPACPU (PK 는 `CPUID`. 관측 49노드/57행)
- 선행: DEPLOYEDASSET
- MERGE ID: `CPUID = view_part_v1.part_pk`

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
| `view_part_v1` | MAXIMO.DPACPU | – | 1:1 (파트 1건 = 행 1건) |
| `view_partmodel_v1` | (보강) | `view_part_v1.partmodel_fk = partmodel_pk` | N:1 |
| `view_vendor_v1` | (보강) | `view_partmodel_v1.vendor_fk = vendor_pk` | N:1 |
| `view_device_v2` | (대상 판정) | `view_part_v1.device_fk = device_pk` | N:1 |

파트 1건이 행 1건이 되므로 소켓 수만큼 행이 생긴다. 관측은 .35
42파트/10장비, .68 66파트/18장비다.

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
| CPU 파트만 | `pm.type_name = 'CPU'` | `view_part_v1` 은 CPU·RAM·Hard Disk·GPU·PCI 를 한 테이블에 담는다 |
| 부모 적재 대상 | `d.type IN ('virtual','physical') AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)` | DEPLOYEDASSET 필터와 일치시킨다 |
| COMPUTER만 | `(d.network_device = false OR d.network_device IS NULL) AND (d.physicalsubtype IS NULL OR d.physicalsubtype NOT IN ('Network Printer','PDU'))` | 다른 ASSETCLASS와 PDU의 자식을 만들지 않는다 |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각 |
| CPUID | CPU ID | BIGINT(19) | N | 직접 | `view_part_v1.part_pk` | Maximo ID로 그대로 사용하며 MERGE 키로 삼는다 |
| CPUNUM | 프로세서 ID | ALN(64) | Y | 직접 | `view_part_v1.slot` | 예: `CPU.Socket.1`. MERGE 매칭에는 사용하지 않는다 |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각 |
| CURRSPEED | 현재 속도 | DECIMAL(10,2) | Y | 원천없음 | – | Device42 는 정격 속도만 제공한다. 기존 수집분은 `0.00` 으로 채움 |
| DESCRIPTION | 설명 | ALN(256) | Y | 변환 | `view_part_v1.description` | 비어 있으면 `view_partmodel_v1.name`. .68 은 전건 비어 있다 |
| IS64BITEN | 64비트 사용 | YORN(1) | N | 상수 | – | 기존 수집분도 전건 `0`. DEFAULTVALUE=0 |
| MAKEMODEL | 제조/모델 | ALN(128) | N | 직접 | `view_partmodel_v1.name` | 없으면 `UNKNOWN`. DEFAULTVALUE=UNKNOWN |
| MANUFACTURER | 제조업체 | ALN(128) | N | 직접 | `view_vendor_v1.name` | `view_partmodel_v1.vendor_fk` 조인. .68 66/66 · .35 42/42. 없으면 `UNKNOWN` |
| MAXSPEED | 최대 속도 | DECIMAL(10,2) | Y | 직접 | `view_partmodel_v1.speed` | 소수 2자리 |
| NODEID | 노드 ID | BIGINT(19) | N | 직접 | `view_part_v1.device_fk` | 부모 DEPLOYEDASSET와 동일한 ID를 직접 사용한다 |
| NUMACTIVECORE | 활성 코어 | INTEGER(12) | Y | 원천없음 | – | Device42 는 활성 코어를 구분하지 않는다 |
| NUMCORE | 코어 | INTEGER(12) | Y | 직접 | `view_partmodel_v1.cores` | .68 66/66 |
| SERIALNUMBER | 일련 번호 | ALN(64) | Y | 원천없음 | – | 양쪽 서버 모두 전건 미보유 (.68 0/66 · .35 0/42) |
| SPEEDUNIT | 속도 단위 | ALN(16) | Y | 직접 | `view_partmodel_v1.speed_unit` | 관측값 `GHz` |
| TLOAMCPUTYPE | 프로세서 유형 | ALN(128) | Y | 원천없음 | – |  |
| TLOAMFAMILY | 프로세서 제품군 | ALN(128) | Y | 원천없음 | – |  |
| VCURRSPEED | 현재 속도 | ALN(32) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |
| VMAXSPEED | 최대 속도 | ALN(32) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

```sql
SELECT
    p.part_pk,
    p.device_fk,
    p.slot,
    p.description,
    pm.name        AS model_name,
    pm.cores,
    pm.speed,
    pm.speed_unit,
    v.name         AS vendor_name
FROM view_part_v1 p
JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
JOIN view_device_v2 d ON d.device_pk = p.device_fk
LEFT JOIN view_vendor_v1 v ON v.vendor_pk = pm.vendor_fk
WHERE pm.type_name = 'CPU'
  AND d.type IN ('virtual', 'physical')
  AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
  AND (d.network_device = false OR d.network_device IS NULL)
  AND (d.physicalsubtype IS NULL OR d.physicalsubtype NOT IN ('Network Printer', 'PDU'))
ORDER BY p.device_fk, p.slot, p.part_pk
```

## 6. 미결

없음.
