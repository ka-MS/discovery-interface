# DPADISK

배치된 자산 컴퓨터 디스크

> Target: MAXIMO.DPADISK · ASSETCLASS: COMPUTER · 구현: [DiskImport](../../../../src/main/java/com/itmsg/device42/pipeline/d42maximo/asset/disk/DiskImport.java) · [DiskQuery](../../../../src/main/java/com/itmsg/device42/source/device42/asset/disk/DiskQuery.java) · [DiskMapper](../../../../src/main/java/com/itmsg/device42/pipeline/d42maximo/asset/disk/DiskMapper.java) · [DpaDiskWriter](../../../../src/main/java/com/itmsg/device42/target/maximo/asset/DpaDiskWriter.java)

> 관측 2026-08-27 · Device42 **양쪽 서버** 192.168.2.68 / 192.168.1.35 · Maximo BLUDB
> 원천 건수는 서버별로 병기한다. 표기는 `.68 / .35` 순이다.

> SQL의 LIMIT/OFFSET은 예시 페이지 값이다. 본체·관계 조회는 Source, 타겟 식별자·값 생성은 Pipeline Mapper가 소유한다.

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : N DPADISK (PK 는 `DISKID`. 관측 61노드/144행)
- 선행: DEPLOYEDASSET
- MERGE ID: `DISKID = view_part_v1.part_pk`

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
| `view_part_v1` | MAXIMO.DPADISK | – | 1:1 (파트 1건 = 행 1건) |
| `view_partmodel_v1` | (보강) | `view_part_v1.partmodel_fk = partmodel_pk` | N:1 |
| `view_vendor_v1` | (보강) | `view_partmodel_v1.vendor_fk = vendor_pk` | N:1 |
| `view_device_v2` | (대상 판정) | `view_part_v1.device_fk = device_pk` | N:1 |

관측은 .35 6파트/6장비, .68 22파트/17장비다. 물리 디스크가 여러 개인
장비에서는 N 이 된다.

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
| 디스크 파트만 | `pm.type_name = 'Hard Disk'` | `view_part_v1` 은 여러 파트 종류를 한 테이블에 담는다 |
| 부모 적재 대상 | `d.type IN ('virtual','physical') AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)` | DEPLOYEDASSET 필터와 일치시킨다 |
| COMPUTER만 | `(d.network_device = false OR d.network_device IS NULL) AND (d.physicalsubtype IS NULL OR d.physicalsubtype NOT IN ('Network Printer','PDU'))` | 다른 ASSETCLASS와 PDU의 자식을 만들지 않는다 |

`view_mountpoint_v2` 은 논리 드라이브 원천이며 DPALOGICALDRIVE 로 간다. 물리 디스크와 혼동하지 않는다.

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| ASSETTAG | 자산 태그 | ALN(64) | Y | 원천없음 | – | `view_part_v1.asset_no` 가 있으나 관측 전건 미보유 |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각 |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각 |
| DESCRIPTION | 설명 | ALN(256) | Y | 변환 | `view_part_v1.description` | 비어 있으면 `view_partmodel_v1.name` |
| DISKID | 디스크 ID | BIGINT(19) | N | 직접 | `view_part_v1.part_pk` | Maximo ID로 그대로 사용하며 MERGE 키로 삼는다 |
| DISKINTERFACE | 디스크 인터페이스 | ALN(32) | Y | 직접 | `view_partmodel_v1.hddtype_name` | .68 9/22 (`SCSI`, `IDE`, `SSD`) · .35 1/6. 기존 수집분은 0/144 |
| DISKTYPE | 디스크 유형 | ALN(32) | Y | 원천없음 | – | 기존 수집분은 `Hard`/`Floppy`/`CD_ROM` 을 쓴다. `media_type_name` 은 .35 에서 전건 `storage`, .68 에서 전건 NULL 이라 대응값이 되지 못한다 |
| EXTERNALDEVICE | 외부 디바이스 | YORN(1) | N | 상수 | – | 기존 수집분 전건 `0` |
| HOTSWAPPABLE | 핫스왑 가능 | YORN(1) | N | 상수 | – | 기존 수집분 전건 `0` |
| MAKEMODEL | 제조/모델 | ALN(128) | Y | 직접 | `view_partmodel_v1.name` | .68 은 실제 모델명(`Samsung SSD 870 QVO 1TB`, `INTEL SSDPEKNW020T8`), .35 는 대부분 `sda NNN GB` 형태다 |
| MANUFACTURER | 제조업체 | ALN(128) | N | 직접 | `view_vendor_v1.name` | `view_partmodel_v1.vendor_fk` 조인. 양쪽 서버 모두 전건 미보유라 사실상 `UNKNOWN` |
| NODEID | 노드 ID | BIGINT(19) | N | 직접 | `view_part_v1.device_fk` | 부모 DEPLOYEDASSET와 동일한 ID를 직접 사용한다 |
| REMOVABLEMEDIA | 이동식 미디어 | YORN(1) | N | 상수 | – | 기존 수집분 전건 `0` |
| SERIALNUMBER | 일련 번호 | ALN(64) | Y | 직접 | `view_part_v1.serial_no` | .68 12/22 · .35 1/6. MERGE 매칭에는 사용하지 않는다 |
| SIZEUNIT | 크기 단위 | ALN(16) | Y | 직접 | `view_partmodel_v1.hdsize_unit` | 관측값 `GB` |
| SYSTEMNAME | 디바이스 | ALN(64) | Y | 원천없음 | – | 기존 수집분은 `1` 로 채워 이름이 아닌 플래그로 쓰인다. 용도 불명 |
| TOTALSPACE | 총 공간 | DECIMAL(10,2) | Y | 직접 | `view_partmodel_v1.hdsize` | 소수 2자리 |
| VTOTALSPACE | 크기 | ALN(32) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |
| WRITECAPABLE | 쓰기 가능 | YORN(1) | N | 상수 | – | 기존 수집분 전건 `0` |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

```sql
SELECT
    p.part_pk,
    p.device_fk,
    p.serial_no,
    p.description,
    pm.name AS model_name,
    pm.hdsize,
    pm.hdsize_unit,
    pm.hddtype_name,
    pm.media_type_name,
    v.name AS vendor_name
FROM view_part_v1 p
JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
JOIN view_device_v2 d ON d.device_pk = p.device_fk
LEFT JOIN view_vendor_v1 v ON v.vendor_pk = pm.vendor_fk
WHERE pm.type_name = 'Hard Disk'
  AND
d.type IN ('virtual', 'physical')
AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
AND (d.network_device = false OR d.network_device IS NULL)
AND (d.physicalsubtype IS NULL OR d.physicalsubtype NOT IN ('Network Printer', 'PDU'))
ORDER BY p.device_fk, pm.name, p.part_pk
LIMIT 1000 OFFSET 0
```

## 6. 미결

없음.
