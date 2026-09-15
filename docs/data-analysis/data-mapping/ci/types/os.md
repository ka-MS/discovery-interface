# OS

> Target: MAXIMO.ACTCI · MAXIMO.ACTCISPEC
> 원천·메타데이터 확인: 2026-09-15 · D42 .68 / .35 · Maximo BLUDB
> 구현: OsCiIntegrate · 상태: 본체·스펙 적재 구현 및 자동 테스트 완료. 관계 자동 적재 미구현. OS → 물리 Computer 단건 INSERT·CI 승격·관련 CI 표시 확인. 속성별 검증은 별도.
> 분류 선택 이유·관계 추천안·미결 근거는 [OS 수집 설계](../../../design/ci/os.md)에 있다.

공통 컬럼 정의는 [ACTCI](../actci.md), [ACTCISPEC](../actcispec.md)가 소유한다.

## 1. 대상과 식별자

| 항목 | 값 |
| --- | --- |
| 대상 | Computer에 연결된 `view_deviceos_v1` 행. 26 / 63건 |
| 분류 | `SYS.OPERATINGSYSTEM` 한 개. 물리·가상을 구분하지 않는다 |
| ACTCINUM | `D42:DEVICEOS:<deviceos_pk>` |
| 스펙 참조 | ACTCINUM·CLASSSTRUCTUREID는 본체와 동일, REFOBJECTID=ACTCIID |
| 관계 | 6절에 원천·분류쌍 매핑 작성. 물리 Computer 단건 승격·UI 확인, 자동 저장 구현은 별도 |

분류명으로 CLASSSTRUCTUREID를 조회한다. 환경별 ID를 상수로 고정하지 않는다.

## 2. 원천과 조회 조건

Computer 수집 필터와 같은 조건으로 부모를 좁힌 뒤 `device_fk`로 조인한다.
`last_discovered`는 OS 원천에 없으므로 부모 Computer에서 가져온다.
문자열은 빈 문자열이 섞여 있어 `NULLIF(TRIM(...), '')`로 정규화한다.

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
SELECT o.deviceos_pk, o.device_fk,
    'D42:DEVICEOS:' || CAST(o.deviceos_pk AS varchar) AS source_id,
    NULLIF(TRIM(o.os_name), '') AS os_name,
    NULLIF(TRIM(o.os_version), '') AS os_version,
    NULLIF(TRIM(o.os_version_no), '') AS os_version_no,
    NULLIF(TRIM(o.os_arch_name), '') AS os_arch_name,
    c.last_discovered
FROM view_deviceos_v1 o
JOIN computer c ON c.device_pk = o.device_fk
ORDER BY o.deviceos_pk
LIMIT %d OFFSET %d
```

2026-09-15 두 서버에서 실행해 통과를 확인했다.

## 3. 본체 매핑

| Target 컬럼 | 한글명 | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- |
| ACTCINUM | 실제 CI 번호 | 변환 | `o.deviceos_pk` | `'D42:DEVICEOS:' || pk` |
| ACTCINAME | 실제 CI 이름 | 직접 | `o.os_name` | 전건 보유. 원문 |
| CLASSSTRUCTUREID | 분류 | 변환 | 상수 분류명 | `SYS.OPERATINGSYSTEM` 조회값 |
| DESCRIPTION | 설명 | 원천없음 | – | OS 원천에 메모 필드가 없다 |
| LASTSCANDT | 최종 발견 시각 | 변환 | `c.last_discovered` | **확정.** 부모 Computer 값. OS 원천에 없음 |
| HASLD | 상세 설명 있음 | 상수 | – | 0 |
| CHANGEBY | 변경자 | 상수 | – | `Device42` |
| CHANGEDATE | 변경 날짜 | 변환 | 매핑 시각 | JVM 기본 시간대 |
| LANGCODE | 언어 코드 | 상수 | – | `KO` |

## 4. 속성 매핑

적재 분류 `SYS.OPERATINGSYSTEM` · **대조 기준 `CI.OS`(CCI00013, 7개)**.
속성 선택은 CI 계열을 기준선으로 삼는다. 선택 근거는 [OS 수집 설계](../../../design/ci/os.md) 3절.
값이 없는 속성은 행을 만들지 않는다.

| ASSETATTRID | 한글 의미 | 값 컬럼 | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- |
| OPERATINGSYSTEM_OSNAME | OS 이름 | ALNVALUE | 직접 | `o.os_name` | 전건. 제조사·제품·버전 합친 정규화 문자열 |
| OPERATINGSYSTEM_NAME | 이름 | ALNVALUE | 직접 | `o.os_name` | CI 기준 속성. OSNAME과 같은 값 |
| OPERATINGSYSTEM_OSVERSION | OS 버전 | ALNVALUE | 직접 | `o.os_version` | 41 / 37건 |
| OPERATINGSYSTEM_KERNELVERSION | 커널 버전 | ALNVALUE | 직접 | `o.os_version_no` | 23 / 19건 |
| OPERATINGSYSTEM_KERNELARCHITECTURE | 커널 아키텍처 | ALNVALUE | 직접 | `o.os_arch_name` | 21 / 19건. **CI 기준 밖 의도적 추가** |

## 5. 미대응·미결

| 항목 | 상태 |
| --- | --- |
| `o.eol`·`o.eos` | 대응 속성 없음. 사업 범위 EOS 관리 대상. ISSUE-11 |
| 제조사 | `view_os_v1.vendor_fk`로 보강 가능하나 분류에 속성 없음. `os_name`에 포함 |
| OPERATINGSYSTEM_VERSIONSTRING | CI 기준 밖. OSVERSION과 중복이라 미채택 |
| KERNELARCHITECTURE 승격 전달 | `SYS.OPERATINGSYSTEM`은 승격 범위에서 `CI.OS`로 매핑되는데 `CI.OS`에 이 속성이 없다. 누락 가능. 미검증. ISSUE-11 |
| MODELOBJECT_CDMSOURCE·SOURCETOKEN | 미채택. CI 계열 분류에 `MODELOBJECT_` 속성이 0개라 승격에서 전달되지 않고, `ACTCINUM`·`CHANGEBY`와 중복이다 |
| 관계 | 6절의 RELATION.INSTALLEDON 매핑. 물리 Computer 단건은 SWAPPED=0 적재·승격·UI 확인. 가상 Computer·자동 저장은 미검증. ISSUE-11 |
| 수집 대상 범위 | Computer 연결분만 추천. 두 서버 비율 30% / 80%로 차이 큼. ISSUE-8 |

## 6. 관계 매핑 — 2026-09-15

[관계 설계](../../../design/ci/relations.md)의 우선 구현 매핑이다.
현재 OsCiIntegrate의 본체·스펙 저장과 별개로, 관계 적재는 미구현이다.

| 의미 | SOURCECI | TARGETCI | RELATIONNUM |
| --- | --- | --- | --- |
| OS 설치 장비 | D42:DEVICEOS:<deviceos_pk> | D42:DEVICE:<device_fk> | RELATION.INSTALLEDON |

출발 SYS.OPERATINGSYSTEM → 도착 SYS.COMPUTERSYSTEM 또는 SYS.VIRTUALCOMPUTERSYSTEM.
N:1, CONTAINMENT=1, REVRELATIONSHIP=1이며 Computer가 상위다.
규칙의 SWAPPED=1을 이유로 양 끝을 다시 뒤집거나 관계 행에 무조건 1을 복사하지 않는다.
[ACTCIRELATION](../actcirelation.md)의 정방향 저장안을 따른다.
2026-09-15 OS → 물리 Computer 한 쌍은 SWAPPED=0으로 INSERT한 뒤 기존 승격 범위에서
CI 및 관계 생성을 확인했다. [검증 결과](../../../knowledge/maximo/computer-ci-relations.md#oscomputer-승격-샘플-검증).
가상 Computer의 자식 승격은 별도 범위 설정·검증이 필요하다.
RUNSON은 실행 의미를 추가하므로 단순 device_fk 연결로 함께 생성하지 않는다.

Computer task 완료 후 OS 본체 배치 저장 뒤 생성한다.
아래 SQL은 두 서버에서 실행 확인했으며, task 내부에서는 이미 읽은 deviceos_pk·device_fk로 같은 쌍을 만든다.

```sql
WITH computer AS (SELECT d.* FROM view_device_v2 d WHERE d.type IN ('physical','virtual')
AND (d.network_device=false OR d.network_device IS NULL)
AND ((d.type='physical' AND d.physicalsubtype IN ('Generic','Rackable','Blade','WorkStation','ThinClient','Laptop'))
OR (d.type='virtual' AND d.virtualsubtype IN ('Internal VM','Amazon EC2 Instance','VMWare','Hyper-V'))))
SELECT DISTINCT 'D42:DEVICEOS:' || CAST(o.deviceos_pk AS varchar) AS sourceci,
       'D42:DEVICE:' || CAST(c.device_pk AS varchar) AS targetci,
       'RELATION.INSTALLEDON' AS relationnum
FROM view_deviceos_v1 o
JOIN computer c ON c.device_pk=o.device_fk
ORDER BY sourceci,targetci;
```
