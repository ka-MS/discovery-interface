# DPADISK

배치된 자산 컴퓨터 디스크

> Target: MAXIMO.DPADISK · ASSETCLASS: COMPUTER · 구현: DpaDiskIntegrate.java

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : N DPADISK (PK 는 `DISKID`. 관측 61노드/144행)
- 선행: DEPLOYEDASSET

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
| `view_part_v1` | MAXIMO.DPADISK | – | 1:1 (파트 1건 = 행 1건) |
| `view_partmodel_v1` | (보강) | `view_part_v1.partmodel_fk = partmodel_pk` | N:1 |
| `view_vendor_v1` | (보강) | `view_partmodel_v1.vendor_fk = vendor_pk` | N:1 |
| MAXIMO.DEPLOYEDASSET | (교차키) | `SOURCEID = view_part_v1.device_fk AND IMPORTSOURCE = 'Device42'` → `NODEID` | N:1 |

관측 6파트/6장비로 장비당 1건이지만, 물리 디스크가 여러 개인 장비에서는 N 이 된다.

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
| 디스크 파트만 | `pm.type_name = 'Hard Disk'` | `view_part_v1` 은 여러 파트 종류를 한 테이블에 담는다 |
| 부모 존재 | 교차키 조회 결과가 있는 것만 | 부모가 없으면 적재할 수 없다 |

`view_mountpoint_v1` 은 논리 드라이브 원천이며 DPALOGICALDRIVE 로 간다. 물리 디스크와 혼동하지 않는다.

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| ASSETTAG | 자산 태그 | ALN(64) | Y | 원천없음 | – | `view_part_v1.asset_no` 가 있으나 관측 전건 미보유 |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각 |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각 |
| DESCRIPTION | 설명 | ALN(256) | Y | 변환 | `view_part_v1.description` | 비어 있으면 `view_partmodel_v1.name` |
| DISKID | 디스크 ID | BIGINT(19) | N | 채번 | – | `NEXT VALUE FOR MAXIMO.DPADISKSEQ`. 테이블 전역 연번이며 노드별이 아니다. INSERT 시에만 발번하고 MATCHED 시 유지한다 |
| DISKINTERFACE | 디스크 인터페이스 | ALN(32) | Y | 직접 | `view_partmodel_v1.hddtype_name` | 관측 6건 중 1건(`SCSI`) 보유. 기존 수집분은 0/144 |
| DISKTYPE | 디스크 유형 | ALN(32) | Y | 미결 | `view_partmodel_v1.media_type_name` | 원천 관측값은 `storage` 뿐이다. 기존 수집분은 `Hard`/`Floppy`/`CD_ROM` 을 쓴다. 값 대응 규칙 필요 |
| EXTERNALDEVICE | 외부 디바이스 | YORN(1) | N | 상수 | – | 기존 수집분 전건 `0` |
| HOTSWAPPABLE | 핫스왑 가능 | YORN(1) | N | 상수 | – | 기존 수집분 전건 `0` |
| MAKEMODEL | 제조/모델 | ALN(128) | Y | 직접 | `view_partmodel_v1.name` | 예: `sda 300 GB`, `VMware Virtual disk SCSI Disk Device` |
| MANUFACTURER | 제조업체 | ALN(128) | N | 직접 | `view_vendor_v1.name` | `view_partmodel_v1.vendor_fk` 조인. 관측 6건 중 0건 보유하여 사실상 `UNKNOWN`. DEFAULTVALUE=UNKNOWN |
| NODEID | 노드 ID | BIGINT(19) | N | 채번 | – | 부모 DEPLOYEDASSET.NODEID. `(SOURCEID, IMPORTSOURCE)` 로 조회 |
| REMOVABLEMEDIA | 이동식 미디어 | YORN(1) | N | 상수 | – | 기존 수집분 전건 `0` |
| SERIALNUMBER | 일련 번호 | ALN(64) | Y | 직접 | `view_part_v1.serial_no` | 관측 6건 중 1건 보유 |
| SIZEUNIT | 크기 단위 | ALN(16) | Y | 직접 | `view_partmodel_v1.hdsize_unit` | 관측값 `GB` |
| SYSTEMNAME | 디바이스 | ALN(64) | Y | 원천없음 | – | 기존 수집분은 `1` 로 채워 이름이 아닌 플래그로 쓰인다. 용도 불명 |
| TOTALSPACE | 총 공간 | DECIMAL(10,2) | Y | 직접 | `view_partmodel_v1.hdsize` | 소수 2자리 |
| VTOTALSPACE | 크기 | ALN(32) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |
| WRITECAPABLE | 쓰기 가능 | YORN(1) | N | 상수 | – | 기존 수집분 전건 `0` |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

```sql
SELECT
    p.device_fk,
    p.serial_no,
    p.description,
    pm.name             AS model_name,
    pm.hdsize,
    pm.hdsize_unit,
    pm.hddtype_name,
    pm.media_type_name,
    v.name              AS vendor_name
FROM view_part_v1 p
JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
LEFT JOIN view_vendor_v1 v ON v.vendor_pk = pm.vendor_fk
WHERE pm.type_name = 'Hard Disk'
ORDER BY p.device_fk, pm.name
```

## 6. 미결

- **MERGE 매칭 키 미정. DPACPU 보다 어렵다.** `DISKID` 는 시퀀스로 발번하므로
  재실행 시 같은 파트를 다시 찾아낼 키가 필요한데, 쓸 만한 원천 값이 없다.
  관측 6건 중 `slot` 은 0건, `serial_no` 는 1건만 값을 가진다.
  `(NODEID, MAKEMODEL)` 은 관측 6건 기준 유일하지만, 같은 모델 디스크를 여러 개
  단 장비에서 충돌한다. 실제로 `sda 300 GB` 처럼 모델명이 용량만 담는 경우가
  있어 충돌 가능성이 높다.
- `DISKTYPE` 값 대응 규칙 미정. 원천 `media_type_name` 은 관측 전건 `storage`
  하나뿐인데 기존 수집분은 `Hard`/`Floppy`/`CD_ROM` 을 쓴다.
- `SYSTEMNAME` 은 기존 수집분이 `1` 로 채워 이름이 아닌 플래그로 쓰인다.
  용도를 확인하기 전에는 채우지 않는다.
