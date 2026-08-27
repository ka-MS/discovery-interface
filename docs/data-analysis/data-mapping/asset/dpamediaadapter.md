# DPAMEDIAADAPTER

배치된 자산 컴퓨터 미디어 어댑터

> Target: MAXIMO.DPAMEDIAADAPTER · ASSETCLASS: COMPUTER · 구현: DpaMediaAdapterIntegrate.java
> 관측 2026-08-27 · Device42 192.168.1.35 / Maximo BLUDB

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : N DPAMEDIAADAPTER (PK 는 `ADAPTERID`. 관측은 39노드/39행이나 스키마는 다건을 허용한다)
- 선행: DEPLOYEDASSET

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
| `view_part_v1` | MAXIMO.DPAMEDIAADAPTER | – | 1:1 (GPU 파트 1건 = 행 1건) |
| `view_partmodel_v1` | (보강) | `view_part_v1.partmodel_fk = partmodel_pk` | N:1 |
| `view_vendor_v1` | (보강) | `view_partmodel_v1.vendor_fk = vendor_pk` | N:1 |
| `view_device_v2` | (대상 판정) | `view_part_v1.device_fk = device_pk` | N:1 |
| MAXIMO.DEPLOYEDASSET | (교차키) | `SOURCEID = view_part_v1.device_fk AND IMPORTSOURCE = 'Device42'` → `NODEID` | N:1 |

COMPUTER 대상에서 GPU 파트는 관측 1장비/1행이다.

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
| GPU 파트만 | `pm.type_name = 'GPU'` | `view_part_v1`은 여러 파트 종류를 한 테이블에 담는다 |
| 부모 적재 대상 | `d.type IN ('virtual','physical') AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)` | DEPLOYEDASSET 필터와 일치시킨다 |
| COMPUTER만 | `(d.network_device = false OR d.network_device IS NULL) AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'Network Printer')` | 다른 ASSETCLASS의 자식을 만들지 않는다 |
| 부모 존재 | 교차키 조회 결과가 있는 것만 | 부모가 없으면 적재할 수 없다 |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| ADAPTERID | 어댑터 | BIGINT(19) | N | 채번 | – | 대리키. 원천 `part_pk` 는 재수집 시 바뀌므로 쓰지 않는다 |
| ASSETTAG | 자산 태그 | ALN(64) | Y | 원천없음 | – | `view_part_v1.asset_no` 관측 0/1 |
| BUSTYPE | 버스 유형 | ALN(32) | Y | 원천없음 | – | `view_partmodel_v1.connectivity_name` 관측 0/1 |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각 |
| CHIPSET | 칩셋 | ALN(64) | Y | 원천없음 | – | 대응 원천이 없다 |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각 |
| DESCRIPTION | 설명 | ALN(256) | Y | 변환 | `view_part_v1.description`, `view_partmodel_v1.description`, `.name` | 앞의 값부터 비어 있지 않은 값을 사용한다. 관측값은 모델명으로 대체된다 |
| MAKEMODEL | 제조/모델 | ALN(128) | N | 직접 | `view_partmodel_v1.name` | 없으면 `UNKNOWN`. DEFAULTVALUE=UNKNOWN |
| MANUFACTURER | 제조업체 | ALN(128) | N | 직접 | `view_vendor_v1.name` | 관측 0/1. 없으면 `UNKNOWN`. DEFAULTVALUE=UNKNOWN |
| MEDIATYPE | 미디어 어댑터 유형 | ALN(32) | Y | 상수 | – | `'Video'`. 기존 수집분도 39/39 전건 `Video` |
| MEMORYTYPE | 메모리 유형 | ALN(32) | Y | 원천없음 | – | `view_partmodel_v1.ramtype` 관측 0/1 |
| NODEID | 노드 ID | BIGINT(19) | N | 채번 | – | 부모 DEPLOYEDASSET.NODEID. `(SOURCEID, IMPORTSOURCE)` 로 조회 |
| RAMSIZE | RAM 크기 | DECIMAL(10,2) | Y | 원천없음 | – | `view_partmodel_v1.ramsize` 관측 0/1 |
| RAMUNIT | RAM 단위 | ALN(16) | Y | 원천없음 | – | 단위 `GB`는 있으나 대응 `ramsize`가 없어 단독 적재하지 않는다 |
| SERIALNUMBER | 일련 번호 | ALN(64) | Y | 직접 | `view_part_v1.serial_no` | 관측 1/1. 현재 값은 PCI 장치 식별자이며 64자 이내다 |
| VRAMSIZE | RAM 크기 | ALN(32) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

```sql
SELECT
    p.device_fk,
    p.serial_no,
    p.description,
    pm.name AS model_name,
    pm.description AS model_description,
    v.name AS vendor_name
FROM view_part_v1 p
JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
JOIN view_device_v2 d ON d.device_pk = p.device_fk
LEFT JOIN view_vendor_v1 v ON v.vendor_pk = pm.vendor_fk
WHERE pm.type_name = 'GPU'
  AND d.type IN ('virtual', 'physical')
  AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
  AND (d.network_device = false OR d.network_device IS NULL)
  AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'Network Printer')
ORDER BY p.device_fk, p.part_pk
```

## 6. 미결

- ISSUE-5 — `ADAPTERID` 채번 범위와 재실행 시 키 유지 규칙이 미정이다.
