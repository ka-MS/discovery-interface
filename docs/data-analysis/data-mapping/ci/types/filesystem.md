# Filesystem

> Target: MAXIMO.ACTCI · MAXIMO.ACTCISPEC
> 원천·메타데이터 확인: 2026-09-15 · D42 .68 / .35 · Maximo BLUDB
> 구현: [FilesystemCiImport](../../../../../src/main/java/com/itmsg/device42/pipeline/d42maximo/ci/filesystem/FilesystemCiImport.java) · [FilesystemCiQuery](../../../../../src/main/java/com/itmsg/device42/source/device42/ci/filesystem/FilesystemCiQuery.java) · [FilesystemCiMapper](../../../../../src/main/java/com/itmsg/device42/pipeline/d42maximo/ci/filesystem/FilesystemCiMapper.java) · [ActCiWriter](../../../../../src/main/java/com/itmsg/device42/target/maximo/ci/ActCiWriter.java) · 상태: 본체·스펙 적재 구현 및 자동 테스트 완료. Computer→Filesystem 관계 저장도 구현됨. 과거 DB 적재 이력과 승격·UI 미검증 범위는 [ACTCIRELATION](../actcirelation.md) 참조.
> 분류 선택 이유·관계 추천안·선별 기준은 [Filesystem 수집 설계](../../../design/ci/filesystem.md)에 있다.

공통 컬럼 정의는 [ACTCI](../actci.md), [ACTCISPEC](../actcispec.md)가 소유한다.

> SQL의 LIMIT/OFFSET은 예시 페이지 값이다. 본체·관계 조회는 Source, 타겟 식별자·값 생성은 Pipeline Mapper가 소유한다.

## 1. 대상과 식별자

| 항목 | 값 |
| --- | --- |
| 대상 | CI Computer 대상에 연결된 마운트 중 overlay·squashfs·efivarfs 제외. NULL fstype은 포함; 배열 조인 후 mountpoint_pk당 한 본체 |
| 분류 | `SYS.FILESYSTEM` 한 개. 선택된 로컬·원격 마운트를 같은 분류로 적재 |
| ACTCINUM | `D42:MOUNTPOINT:<mountpoint_pk>` |
| 스펙 참조 | ACTCINUM·CLASSSTRUCTUREID는 본체와 동일, REFOBJECTID=ACTCIID |
| 관계 | [Computer 출발 관계 매핑](device.md#7-관계-매핑--2026-09-15)에 원천·분류쌍·저장 구현 완료. `ci-relation` 운영 적재·멱등성 검증 완료; CI 승격·UI 검증은 별도 |

`SYS.LOCALFILESYSTEM`을 쓰지 않는다. 원천에 nfs·nfs4·overlay·VMFS·squashfs가 섞여 있어
로컬 분류가 사실과 어긋난다. 두 분류의 속성 33개는 완전히 동일하며 계층상 형제다.

**승격 전제:** Maximo 기본 구성은 `CI.FILESYSTEM`을 `SYS.LOCALFILESYSTEM`에만 매핑한다.
`SYS.FILESYSTEM`으로 적재한 CI를 승격하려면 승격 범위에 매핑 행이 있어야 한다.
2026-09-15 추가했다(`CITEMPLATE` 164번). 설정은 [CI 승격 범위](../../../knowledge/maximo/ci-promotion-scope.md).

## 2. 원천과 조회 조건

`device_fks`가 배열이므로 `= ANY(...)`로 조인한다. 관측상 원소가 항상 1개지만
같은 원천 PK가 여러 행이 되는 것을 막기 위해 `DISTINCT ON`을 유지한다. 근거는 ISSUE-9.

```sql
WITH computer AS (
    SELECT d.device_pk, d.last_discovered
    FROM view_device_v2 d
    WHERE
d.type IN ('physical', 'virtual')
AND (d.network_device = false OR d.network_device IS NULL)
AND (
    (d.type = 'physical' AND d.physicalsubtype IN ('Generic', 'Rackable', 'Blade', 'WorkStation', 'ThinClient', 'Laptop'))
    OR (d.type = 'virtual' AND d.virtualsubtype IN ('Internal VM', 'Amazon EC2 Instance', 'VMWare', 'Hyper-V'))
)
)
SELECT DISTINCT ON (m.mountpoint_pk)
    m.mountpoint_pk, c.device_pk AS device_fk,
    NULLIF(TRIM(m.mountpoint), '') AS mountpoint,
    NULLIF(TRIM(m.fstype_name), '') AS fstype_name,
    NULLIF(TRIM(m.label), '') AS label,
    m.capacity, m.free_capacity, c.last_discovered
FROM view_mountpoint_v2 m
JOIN computer c ON c.device_pk = ANY(m.device_fks)
WHERE (m.fstype_name IS NULL OR m.fstype_name NOT IN ('overlay', 'squashfs', 'efivarfs'))
ORDER BY m.mountpoint_pk, c.device_pk
LIMIT 1000 OFFSET 0
```

과거 2026-09-15 조건의 관측은 69 / 60건이다. 현재 조건은 devtmpfs를 포함하도록 변경된 뒤의 SQL이며, 2026-09-17 관측은 78 / 70건이다. 이번 문서 대조에서 DB 재조회는 하지 않았다.

## 3. 본체 매핑

| Target 컬럼 | 한글명 | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- |
| ACTCINUM | 실제 CI 번호 | 변환 | `m.mountpoint_pk` | `'D42:MOUNTPOINT:' || pk` |
| ACTCINAME | 실제 CI 이름 | 직접 | `m.mountpoint` | 전건. 192자 초과 가능. 5절 |
| CLASSSTRUCTUREID | 분류 | 변환 | 상수 분류명 | `SYS.FILESYSTEM` 조회값 |
| DESCRIPTION | 설명 | 원천없음 | – | 원천에 메모 필드가 없다 |
| LASTSCANDT | 최종 발견 시각 | 변환 | `c.last_discovered` | **확정.** 부모 Computer 값. 마운트 원천에 없음 |
| HASLD | 상세 설명 있음 | 상수 | – | 0 |
| CHANGEBY | 변경자 | 상수 | – | `Device42` |
| CHANGEDATE | 변경 날짜 | 변환 | 매핑 시각 | JVM 기본 시간대 |
| LANGCODE | 언어 코드 | 상수 | – | `KO` |

## 4. 속성 매핑

적재 분류 `SYS.FILESYSTEM` · **대조 기준 `CI.FILESYSTEM`(CCI00026, 16개)**.
CI 기준 네 개에 `MODELOBJECT_LABEL`을 더한 다섯 개다. 선택 근거는 [Filesystem 수집 설계](../../../design/ci/filesystem.md) 3절.
해당 분류의 CLASSSPEC이 있는 속성은 값이 없어도 행을 만들어 값 컬럼을 NULL로 동기화한다.

| ASSETATTRID | 한글 의미 | 값 컬럼 | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- |
| FILESYSTEM_MOUNTPOINT | 마운트 지점 | ALNVALUE | 직접 | `m.mountpoint` | 전건. 254자 초과 시 처리 미결 |
| FILESYSTEM_TYPE | 파일시스템 유형 | ALNVALUE | 직접 | `m.fstype_name` | 115 / 141건 |
| FILESYSTEM_CAPACITY | 용량 | NUMVALUE | 변환 | `m.capacity` | 114 / 140건. MB 해석. 5절 |
| FILESYSTEM_AVAILABLESPACE | 사용 가능 공간 | NUMVALUE | 변환 | `m.free_capacity` | 106 / 129건. MB 해석 |
| MODELOBJECT_LABEL | 레이블 | ALNVALUE | 직접 | `m.label` | 24 / 8건. **CI 기준 밖 의도적 추가** |

## 5. 미대응·미결

| 항목 | 상태 |
| --- | --- |
| 컨테이너·가상 파일시스템 | **확정.** `MaximoSourcePolicy.CI_FILESYSTEM` 상수로 `overlay`·`squashfs`·`efivarfs`를 원천 조회에서 제외한다. `.68` 78건, `.35` 70건(2026-09-17). `devtmpfs`는 2026-09-17 수집 대상으로 되돌렸다 — 경로가 `/dev`로 고정돼 재기동 시 원천 PK가 바뀌는 문제가 없다 |
| 마운트 경로 길이 | 컨테이너 경로가 약 130자다. ACTCINAME 192자·ALNVALUE 254자 한계에 근접. 절단·생략 규칙 필요. ISSUE-11 |
| 용량 단위 | **확정.** `MEASUREUNITID='MBYTE'`를 지정한다 |
| `m.filesystem` | 93 / 133건 보유하나 대응 속성 없음. 추가 등록 필요. 이번 범위 제외 추천 |
| MODELOBJECT_LABEL 승격 전달 | CI 계열 분류에 `MODELOBJECT_` 속성이 0개다. 누락될 수 있다. 미검증. ISSUE-11 |
| 관계 | [Computer 출발 관계 매핑](device.md#7-관계-매핑--2026-09-15)에 RELATION.CONTAINS의 원천 SQL·방향 정의. 배열 연결 쌍 보존 필요. 관계 저장·운영 멱등성 검증 완료, CI 승격·UI 검증 미완료. ISSUE-11 |
| 승격 1:N 매핑 | `CI.FILESYSTEM`에 ACTCI 분류 둘이 걸린 유일한 경우. 동작 미검증. ISSUE-11 |
