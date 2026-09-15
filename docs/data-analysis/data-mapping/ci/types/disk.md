# Disk

> Target: MAXIMO.ACTCI · MAXIMO.ACTCISPEC
> 원천·메타데이터 확인: 2026-09-15 · D42 .68 / .35 · Maximo BLUDB
> 구현: DiskCiIntegrate · 상태: 본체·스펙 적재 구현 및 자동 테스트 완료. 관계 미적재. 실제 Maximo 적재·UI 검증은 미완료.
> 분류 선택 이유·관계 추천안·미결 근거는 [Disk 수집 설계](../../../design/ci/disk.md)에 있다.

공통 컬럼 정의는 [ACTCI](../actci.md), [ACTCISPEC](../actcispec.md)가 소유한다.

## 1. 대상과 식별자

| 항목 | 값 |
| --- | --- |
| 대상 | `view_partmodel_v1.type_name='Hard Disk'`인 `view_part_v1` 행. 23 / 19건 전건 Computer 연결 |
| 분류 | `DEV.DISKDRIVE` 한 개 |
| ACTCINUM | `D42:PART:<part_pk>` |
| 스펙 참조 | ACTCINUM·CLASSSTRUCTUREID는 본체와 동일, REFOBJECTID=ACTCIID |
| 관계 | 확정하지 않는다. 추천안은 설계 문서 |

`view_part_v1`에는 `type_name`이 없다. 파트 종류는 `view_partmodel_v1` 조인으로 판정한다.
`p.pcount`는 두 서버 모두 전건 1이므로 수량으로 해석하지 않는다. 한 행이 디스크 한 개다.

## 2. 원천과 조회 조건

```sql
WITH computer AS (
    SELECT d.device_pk, d.last_discovered
    FROM view_device_v2 d
    WHERE d.type IN ('physical', 'virtual')
      AND (d.network_device = false OR d.network_device IS NULL)
      AND (
          (d.type = 'physical' AND d.physicalsubtype IN
              ('Generic', 'Rackable', 'Blade', 'WorkStation', 'ThinClient', 'Laptop'))
          OR
          (d.type = 'virtual' AND d.virtualsubtype IN
              ('Internal VM', 'Amazon EC2 Instance', 'VMWare', 'Hyper-V'))
      )
)
SELECT p.part_pk, p.device_fk,
    'D42:PART:' || CAST(p.part_pk AS varchar) AS source_id,
    NULLIF(TRIM(pm.name), '') AS model,
    NULLIF(TRIM(p.serial_no), '') AS serial_no,
    NULLIF(TRIM(p.description), '') AS description,
    pm.hdsize, NULLIF(TRIM(pm.hdsize_unit), '') AS hdsize_unit,
    NULLIF(TRIM(pm.hddtype_name), '') AS hddtype_name,
    c.last_discovered
FROM view_part_v1 p
JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
JOIN computer c ON c.device_pk = p.device_fk
WHERE pm.type_name = 'Hard Disk'
ORDER BY p.part_pk
LIMIT %d OFFSET %d
```

2026-09-15 두 서버에서 실행해 통과를 확인했다.

## 3. 본체 매핑

| Target 컬럼 | 한글명 | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- |
| ACTCINUM | 실제 CI 번호 | 변환 | `p.part_pk` | `'D42:PART:' || pk` |
| ACTCINAME | 실제 CI 이름 | 직접 | `pm.name` | 모델명. `sda 500 GB` 같은 장치명도 있다 |
| CLASSSTRUCTUREID | 분류 | 변환 | 상수 분류명 | `DEV.DISKDRIVE` 조회값 |
| DESCRIPTION | 설명 | 직접 | `p.description` | 값 분포 미확인 |
| LASTSCANDT | 최종 발견 시각 | 변환 | `c.last_discovered` | **확정.** 부모 Computer 값. Part 원천에 없음 |
| HASLD | 상세 설명 있음 | 상수 | – | 0 |
| CHANGEBY | 변경자 | 상수 | – | `Device42` |
| CHANGEDATE | 변경 날짜 | 변환 | 매핑 시각 | JVM 기본 시간대 |
| LANGCODE | 언어 코드 | 상수 | – | `KO` |

## 4. 속성 매핑

적재 분류 `DEV.DISKDRIVE` · **대조 기준 없음**. `CIROOT` 계열에 디스크 대응 분류가 없어
네 유형 중 유일하게 기준선 없이 ACTCI 쪽만 보고 골랐다.
근거는 [Disk 수집 설계](../../../design/ci/disk.md) 3절.
값이 없는 속성은 행을 만들지 않는다.

| ASSETATTRID | 한글 의미 | 값 컬럼 | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- |
| MEDIAACCESSDEVICE_MODEL | 모델 | ALNVALUE | 직접 | `pm.name` | 23 / 19건 |
| MEDIAACCESSDEVICE_SERIALNUMBER | 일련번호 | ALNVALUE | 직접 | `p.serial_no` | 13 / 3건. 공백 제외 |
| DISKDRIVE_DISKSIZE | 디스크 크기 | NUMVALUE | 변환 | `pm.hdsize`, `pm.hdsize_unit` | GB는 그대로, TB는 ×1024. 단위 코드는 `GBYTE` 고정 |

## 5. 미대응·미결

| 항목 | 상태 |
| --- | --- |
| `p.firmware` | **전건 빈 문자열.** `DISKDRIVE_REVISION`에 채울 값이 없다 |
| `pm.vendor_fk` | **전건 없음.** `MEDIAACCESSDEVICE_MANUFACTURER`·`DISKDRIVE_VENDOR` 채울 수 없다 |
| `pm.media_type_name` | **전건 없음.** `MEDIAACCESSDEVICE_TYPE` 채울 수 없다 |
| `pm.hddtype_name` | 10 / 2건. `SSD`는 1 / 0건뿐이라 `DISKDRIVE_ISSOLIDSTATE` 판정 근거가 부족하다 |
| DISKSIZE 단위 | **확정.** `MEASUREUNIT`에 `TBYTE`가 없어(2026-09-15 확인) TB는 GB로 환산한다. 미지원 단위는 용량 스펙만 생략한다 |
| 식별자 접두어 | `view_part_v1`이 CPU·RAM과 공용이라 `PART` 대 `DISK` 선택 필요. ISSUE-11 |
| ACTCINAME 중복 | 같은 모델 디스크가 같은 이름을 갖는다. 슬롯·장비명 부가 검토. ISSUE-11 |
| 원천 커버리지 | 표본 Computer 95 / 85대 중 18 / 19대만 디스크 보유. ISSUE-8 |
| 승격 불가 | `DEV.DISKDRIVE`가 `CITEMPLATE`에 0행이다. CI로 승격할 수 없다. ISSUE-8 |
| 관계 | `RELATION.CONTAINS`(Computer→Disk) 추천. `USEWITH`가 CI라 미검증. ISSUE-11 |
