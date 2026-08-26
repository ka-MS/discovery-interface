# DPACPU

배치된 자산 컴퓨터 프로세서

> Target: MAXIMO.DPACPU · ASSETCLASS: COMPUTER · 구현: DpaCpuIntegrate.java

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : N DPACPU (관측 49노드/57행)
- 선행: DEPLOYEDASSET

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
| `view_part_v1` | MAXIMO.DPACPU | – | 1:1 (파트 1건 = 행 1건) |
| `view_partmodel_v1` | (보강) | `view_part_v1.partmodel_fk = partmodel_pk` | N:1 |
| `view_vendor_v1` | (보강) | `view_partmodel_v1.vendor_fk = vendor_pk` | N:1 |
| MAXIMO.DEPLOYEDASSET | (교차키) | `SOURCEID = view_part_v1.device_fk AND IMPORTSOURCE = 'Device42'` → `NODEID` | N:1 |

파트 1건이 행 1건이 되므로 소켓 수만큼 행이 생긴다. 관측 43파트/11장비.

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
| CPU 파트만 | `pm.type_name = 'CPU'` | `view_part_v1` 은 CPU·RAM·Hard Disk·GPU·PCI 를 한 테이블에 담는다 |
| 부모 존재 | 교차키 조회 결과가 있는 것만 | 부모가 없으면 적재할 수 없다 |

부모 필터(`type IN ('virtual','physical')` 등)와 일치시켜야 교차키 실패가 생기지 않는다. DPACOMPUTER 의 ISSUE-4 와 같은 문제를 반복하지 않도록 주의한다.

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각 |
| CPUID | CPU ID | BIGINT(19) | N | 채번 | – | 대리키. 원천 `part_pk` 는 재수집 시 바뀌므로 쓰지 않는다 |
| CPUNUM | 프로세서 ID | ALN(64) | Y | 직접 | `view_part_v1.slot` | 예: `CPU.Socket.1`. 기존 수집분은 0/57 미사용 |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각 |
| CURRSPEED | 현재 속도 | DECIMAL(10,2) | Y | 원천없음 | – | Device42 는 정격 속도만 제공한다. 기존 수집분은 `0.00` 으로 채움 |
| DESCRIPTION | 설명 | ALN(256) | Y | 변환 | `view_part_v1.description` | 비어 있으면 `view_partmodel_v1.name` |
| IS64BITEN | 64비트 사용 | YORN(1) | N | 상수 | – | 기존 수집분도 전건 `0`. DEFAULTVALUE=0 |
| MAKEMODEL | 제조/모델 | ALN(128) | N | 직접 | `view_partmodel_v1.name` | 없으면 `UNKNOWN`. DEFAULTVALUE=UNKNOWN |
| MANUFACTURER | 제조업체 | ALN(128) | N | 직접 | `view_vendor_v1.name` | `view_partmodel_v1.vendor_fk` 조인. 관측 43건 중 42건 보유. 없으면 `UNKNOWN`. DEFAULTVALUE=UNKNOWN |
| MAXSPEED | 최대 속도 | DECIMAL(10,2) | Y | 직접 | `view_partmodel_v1.speed` | 소수 2자리 |
| NODEID | 노드 ID | BIGINT(19) | N | 채번 | – | 부모 DEPLOYEDASSET.NODEID. `(SOURCEID, IMPORTSOURCE)` 로 조회 |
| NUMACTIVECORE | 활성 코어 | INTEGER(12) | Y | 원천없음 | – | Device42 는 활성 코어를 구분하지 않는다 |
| NUMCORE | 코어 | INTEGER(12) | Y | 직접 | `view_partmodel_v1.cores` |  |
| SERIALNUMBER | 일련 번호 | ALN(64) | Y | 원천없음 | – | 관측 43건 중 0건 보유 |
| SPEEDUNIT | 속도 단위 | ALN(16) | Y | 직접 | `view_partmodel_v1.speed_unit` | 관측값 `GHz` |
| TLOAMCPUTYPE | 프로세서 유형 | ALN(128) | Y | 원천없음 | – |  |
| TLOAMFAMILY | 프로세서 제품군 | ALN(128) | Y | 원천없음 | – |  |
| VCURRSPEED | 현재 속도 | ALN(32) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |
| VMAXSPEED | 최대 속도 | ALN(32) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

```sql
SELECT
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
LEFT JOIN view_vendor_v1 v ON v.vendor_pk = pm.vendor_fk
WHERE pm.type_name = 'CPU'
ORDER BY p.device_fk, p.slot
```

## 6. 미결

- `CPUID` 채번 규칙 미정. 기존 수집분은 전역 연번을 쓴다. 노드 내 연번으로 할지 전역 연번으로 할지 정해야 한다.
- `DESCRIPTION` 은 관측 43건 중 일부만 값이 있다. 비어 있을 때 모델명으로 대체할지 NULL 로 둘지 정해야 한다.
