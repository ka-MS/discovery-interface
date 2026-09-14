# Computer CI

> Target: MAXIMO.ACTCI · MAXIMO.ACTCISPEC
> 원천·메타데이터 확인: 2026-09-14 · D42 .68 / .35 · Maximo BLUDB
> 구현: ComputerCiIntegrate · 상태: 수집·ACTCI·ACTCISPEC 저장 및 자동 테스트 완료. 실제 Maximo 적재·UI 검증은 미완료.
> 실행 설정·추가 속성 등록·현재 처리 동작은 [실행 준비](computer-run.md)를 따른다. 미대응 항목은 아래 표에 구분한다.

## 1. 대상과 식별자

물리 Computer와 VM을 장비당 하나의 ACTCI로 수집한다. 이번 범위는 본체와 요약 스펙이다.
개별 CPU·Disk·OS·Interface 등의 CI와 관계는 후속 유형에서 정의한다.
DPA 적재 결과나 변환 규칙에 의존하지 않는다.

| 항목 | 규칙 |
| --- | --- |
| 물리 분류 | `SYS.COMPUTERSYSTEM` |
| VM 분류 | `SYS.VIRTUALCOMPUTERSYSTEM` |
| 원천 키 / ACTCINUM | `D42:DEVICE:<device_pk>` |
| ACTCIID | Maximo 숫자 채번. 원천 PK를 대입하지 않음 |
| 갱신 | 같은 ACTCINUM은 기존 ACTCIID 유지. device_pk가 바뀌면 신규 CI |
| 스펙 참조 | ACTCINUM·CLASSSTRUCTUREID는 본체와 동일, REFOBJECTID=ACTCIID |
| 스펙 선택 | CI.COMPUTERSYSTEM의 18개를 대조 기준으로 사용하고 BIOS·CPU 코어 수 추가 |

분류명으로 CLASSSTRUCTUREID를 조회한다. 환경별 ID를 상수로 고정하지 않는다.
CI 분류의 CLASSSPECID를 ACTCISPEC에 복사하지 않는다.

## 2. 원천과 조회 조건

| Source | 용도 | 연결 |
| --- | --- | --- |
| `view_device_v2 d` | 본체·메모리·CPU 요약·BIOS·VM ID | device_pk가 수집 단위 |
| `view_hardware_v2 h` | 모델 | h.hardware_pk=d.hardware_fk |
| `view_vendor_v1 v / b` | 장비 / BIOS 제조사 | v.vendor_pk=h.vendor_fk / b.vendor_pk=d.bios_vendor_fk |
| `view_part_v1 p` + `view_partmodel_v1 pm` | CPU 모델·아키텍처 | p.device_fk=d.device_pk, pm.partmodel_pk=p.partmodel_fk, pm.type_name='CPU' |
| `view_netport_v1 n` | 기본 포트 MAC | n.device_fk=d.device_pk, n.is_default=true |

보강 정보는 LEFT JOIN한다. CPU·포트는 먼저 장비별로 집계하여 본체 행을 늘리지 않는다.

이번 매핑은 다음 서브타입을 수집 대상으로 명시한다. D42 전체 유형의 목록은 아니다.

| 조건 | 포함 값 |
| --- | --- |
| 공통 | type이 physical / virtual이고 network_device가 false 또는 NULL |
| physical | Generic, Rackable, Blade, WorkStation, ThinClient, Laptop |
| virtual | Internal VM, Amazon EC2 Instance, VMWare, Hyper-V |

네트워크 장비·프린터·설비·컨테이너·cluster·unknown은 이 범위에 포함되지 않는다.
서브타입 미지정·새로운 값은 자동으로 Computer에 편입하지 않고 대상 정의를 보완한다.
`virtual_host`는 호스트 역할을 나타내므로 물리/가상 분류를 뒤집는 조건으로 사용하지 않는다.
이 조건은 이번에 작성한 수집 규칙이며 기존 asset 구현의 필터와 별개다.

## 3. ACTCI 본체 매핑

물리 타입·길이·필수 여부는 [ACTCI 공통 표](../actci.md#4-컬럼-매핑)를 따른다.
아래 구분은 매핑 상태이며, 미결 행은 적재 기본값으로 해석하지 않는다.

| Target 컬럼 | 한글명 | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- |
| ACTCIID | 고유 ID | 채번 | Maximo ACTCISEQ | 신규만 NEXT VALUE 사용. 기존 ACTCINUM의 ID 유지 |
| ACTCINUM | 실제 CI 번호 | 변환 | d.device_pk | `'D42:DEVICE:' \|\| CAST(d.device_pk AS varchar)` |
| ACTCINAME | 실제 CI 이름 | 직접 | d.name | 원문; 임의 대문자 변환·절단 없음 |
| CLASSSTRUCTUREID | 분류 | 변환 | d.type → 분류명 | 1절의 ACTCI 적용 분류 조회 |
| DESCRIPTION | 설명 | 직접 | d.notes | 원문; 상세 설명으로 자동 분리하지 않음 |
| LASTSCANDT | 최종 발견 시각 | 변환 | d.last_discovered | ci.zone-id 시간대로 변환. 누락·파싱 오류는 현재 예외 전달 |
| HASLD | 상세 설명 있음 | 상수 | — | 상세 설명 미사용 시 0 |
| CHANGEBY | 변경자 | 상수 | ci.change-by | 실행 설정; 실제 Maximo 계정 지정 |
| CHANGEDATE | 변경 날짜 | 변환 | 저장 시각 | ci.zone-id 기준. 본체·스펙이 같은 시각 사용 |
| LANGCODE | 언어 코드 | 상수 | ci.lang-code | 실행 설정; 원천 이름의 언어로 추정하지 않음 |
| GUID / CCIDISGUID | 발견 ID / 통합 ID | 미결 | 직접 대응 미확정 | 신규는 미설정, 기존 값은 유지. d.uuid는 UUID 스펙에 대응 |
| EXTENDEDINSTANCES | 확장 인스턴스 | 원천없음 | — | 이번 매핑에서 사용하지 않음 |
| PLUSPCUSTOMER | 기본 고객 | 원천없음 | — | D42 customer_fk와 Maximo 고객 코드 대응 없음 |

## 4. ACTCISPEC 값 매핑

적용 분류는 두 ACTCI Computer 분류다. 아래 ASSETATTRID는 공통 접두어 `COMPUTERSYSTEM_`를 생략했다.
ALNVALUE는 문자열, NUMVALUE는 숫자다. 한 행에서 값 컬럼 하나만 사용한다.
`d/h/v/b`는 2절 별칭이며 파생 필드는 5절 SQL의 반환값이다.

### CI 기준 18개

| ASSETATTRID | 한글 의미 | 값 컬럼 | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- |
| NAME | 이름 | ALNVALUE | 직접 | d.name | 본체 이름과 같은 원천 |
| SERIALNUMBER | 시리얼 | ALNVALUE | 직접 | d.serial_no | 원문 |
| UUID | 장비 UUID | ALNVALUE | 직접 | d.uuid | 갱신 키로 사용하지 않음 |
| MANUFACTURER | 제조사 | ALNVALUE | 직접 | v.name | 없는 값을 UNKNOWN으로 만들지 않음 |
| MODEL | 모델 | ALNVALUE | 직접 | h.name | VM에 모델이 없으면 보강값 없음 |
| MEMORYSIZE | 메모리 총량 | NUMVALUE | 직접 | d.ram + d.ram_size_type | 숫자 보존; GB→GBYTE, MB→MBYTE |
| NUMCPUS | CPU 수 | NUMVALUE | 직접 | d.total_cpus | D42가 해당 장비에 보고한 CPU 수 |
| CPUSPEED | CPU 속도 | NUMVALUE | 직접 | d.cpu_speed + d.hz | 숫자 보존; GHz→GHZ, MHz→MHZ |
| CPUTYPE | CPU 모델 | ALNVALUE | 변환 | pm.name → cpu_type | 비어 있지 않은 모델명이 정확히 1종일 때만 사용 |
| ARCHITECTURE | CPU 아키텍처 | ALNVALUE | 변환 | p.details→architecture | CPU 파트의 비어 있지 않은 값이 정확히 1종일 때만 사용; os_architecture의 32/64-bit와 구분 |
| PRIMARYMACADDRESS | 대표 MAC | ALNVALUE | 변환 | 기본 포트 n.hwaddress | is_default=true인 포트가 정확히 1개일 때 사용; 다른 포트에서 임의 선택하지 않음 |
| TYPE | CDM 유형 | ALNVALUE | 상수 | — | `ComputerSystem`; d.type의 physical/virtual과 구분 |
| VIRTUAL | 가상 여부 | ALNVALUE | 변환 | d.type | virtual→`true`, physical→`false` |
| VMID | VM 식별자 | ALNVALUE | 직접 | d.vm_manager_int_id | virtual에만 적용. 관리자 내부 ID이며 ACTCINUM과 별개 |
| FQDN | 정규 도메인 이름 | ALNVALUE | 미결 | 직접 대응 미확정 | d.name을 FQDN으로 간주하지 않음. 클라우드 DNS 필드는 범위·선택 기준 별도 |
| MANAGEDSYSTEMNAME | 관리 시스템 이름 | ALNVALUE | 원천없음 | 이번 조회 원천에 전용 대응 없음 | NAME을 복제하지 않음 |
| SIGNATURE | 시스템 서명 | ALNVALUE | 미결 | IP·MAC 조합 후보 | 대표 IP와 서명 생성 규칙 미정; 문자열 sourceId와 별개 |
| SYSTEMBOARDUUID | 시스템 보드 UUID | ALNVALUE | 원천없음 | 이번 조회 원천에 전용 대응 없음 | 일반 장비 UUID를 보드 UUID로 단정하지 않음 |

### 추가 수집

| ASSETATTRID | 한글 의미 | 값 컬럼 | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- |
| BIOSMANUFACTURER | BIOS 제조사 | ALNVALUE | 직접 | b.name | 기존 ACTCI 스펙 사용; BIOSNAME에 제조사명을 넣지 않음 |
| ROMVERSION | BIOS 버전 | ALNVALUE | 직접 | d.bios_version | 기존 ACTCI 스펙 사용; BIOS revision·firmware revision과 합치지 않음 |
| CPUCORESINSTALLED | 장비 총 코어 수 | NUMVALUE | 변환 | d.total_cpus × d.core_per_cpu | bigint 곱. 하나라도 NULL이면 NULL; threads_per_core를 곱하지 않음 |
| BIOSRELEASEDATE | BIOS 출시일 원문 | ALNVALUE | 직접 | d.bios_release_date | 추가 정의할 속성. 원문 보존; 아래 등록 전제 참조 |

코어 수는 D42의 “CPU 수 × CPU당 코어 수”로 계산한 해당 장비의 보고 총량이다.
VM에서는 VM에 보고된 구성으로 해석하며 호스트의 물리 코어 수나 활성 코어 수를 뜻하지 않는다.
CPU 파트 수·모델별 cores 합계로 대체하지 않는다.

**단위:** MEMORYSIZE·CPUSPEED의 ACTCI 템플릿 단위는 현재 공란이다.
위 단위 코드를 ACTCISPEC.MEASUREUNITID에 명시하고 수치는 환산하지 않는 것이 이번 매핑 규칙이다.
GBYTE·MBYTE·GHZ·MHZ는 Maximo에 등록되어 있다. 단위 누락·미지원 표기에는 임의 단위를 붙이지 않는다.
IBM CDM 원래 단위와 동일하다고 가정하지 않으며 승격·후속 연계도 값과 단위를 함께 처리해야 한다.

**추가 속성 등록 전제:** `COMPUTERSYSTEM_BIOSRELEASEDATE`는 이번 문서에서 정한 ALN 속성명이며 현재 미등록이다.
ASSETATTRIBUTE와 두 ACTCI 분류의 CLASSSPEC·ACTCI용 적용 설정을 등록한 후 사용한다.
기존 `COMPUTERSYSTEM_BIOSDATE`는 NUMERIC이고 날짜 인코딩 규약은 확인되지 않아 사용하지 않는다.
이는 ACTCISPEC 테이블의 새 컬럼이 아니라 속성 정의와 값 행을 추가하는 작업이다.

## 5. 조회 SQL

### D42 원천

아래 SQL은 수집·매핑용 SELECT이며 미결 항목의 값을 생성하지 않는다.
model_count·arch_count·default_port_count는 복수 값 때문에 보강을 생략했는지 구분하는 진단값이며 스펙이 아니다.

```sql
WITH computer AS (
    SELECT d.*
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
), cpu AS (
    SELECT p.device_fk,
        COUNT(DISTINCT NULLIF(TRIM(pm.name), '')) AS model_count,
        MIN(NULLIF(TRIM(pm.name), '')) AS cpu_model,
        COUNT(DISTINCT NULLIF(TRIM(p.details->>'architecture'), '')) AS arch_count,
        MIN(NULLIF(TRIM(p.details->>'architecture'), '')) AS architecture
    FROM view_part_v1 p
    JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
    JOIN computer d ON d.device_pk = p.device_fk
    WHERE pm.type_name = 'CPU'
    GROUP BY p.device_fk
), primary_port AS (
    SELECT n.device_fk, COUNT(*) AS default_port_count,
        MIN(NULLIF(TRIM(n.hwaddress), '')) AS primary_mac
    FROM view_netport_v1 n
    JOIN computer d ON d.device_pk = n.device_fk
    WHERE n.is_default = true
    GROUP BY n.device_fk
)
SELECT d.device_pk, d.type,
    'D42:DEVICE:' || CAST(d.device_pk AS varchar) AS source_id,
    CASE d.type WHEN 'physical' THEN 'SYS.COMPUTERSYSTEM'
                WHEN 'virtual' THEN 'SYS.VIRTUALCOMPUTERSYSTEM' END AS classification_id,
    d.name, d.notes, d.serial_no, d.uuid, d.last_discovered,
    h.name AS model, v.name AS manufacturer,
    d.ram, d.ram_size_type, d.total_cpus, d.core_per_cpu,
    CAST(d.total_cpus AS bigint) * d.core_per_cpu AS total_cores,
    d.cpu_speed, d.hz AS cpu_speed_unit,
    CASE WHEN cpu.model_count = 1 THEN cpu.cpu_model END AS cpu_type,
    CASE WHEN cpu.arch_count = 1 THEN cpu.architecture END AS architecture,
    CASE WHEN pp.default_port_count = 1 THEN pp.primary_mac END AS primary_mac,
    'ComputerSystem' AS system_type,
    CASE d.type WHEN 'virtual' THEN 'true' ELSE 'false' END AS is_virtual,
    CASE WHEN d.type = 'virtual' THEN d.vm_manager_int_id END AS vm_id,
    b.name AS bios_manufacturer, d.bios_version, d.bios_release_date,
    cpu.model_count, cpu.arch_count, pp.default_port_count
FROM computer d
LEFT JOIN view_hardware_v2 h ON h.hardware_pk = d.hardware_fk
LEFT JOIN view_vendor_v1 v ON v.vendor_pk = h.vendor_fk
LEFT JOIN view_vendor_v1 b ON b.vendor_pk = d.bios_vendor_fk
LEFT JOIN cpu ON cpu.device_fk = d.device_pk
LEFT JOIN primary_port pp ON pp.device_fk = d.device_pk
ORDER BY d.device_pk;
```

### Maximo 분류·스펙 템플릿

속성별 기대 자료형은 4절 값 컬럼과 대조한다. 동일 분류·속성·섹션의 템플릿이 하나이고
ACTCI 적용 설정이 있는지 확인한다. BIOSRELEASEDATE는 기준정보 등록 전 결과에 나오지 않는다.
4절에서 미결·원천없음인 속성은 템플릿이 조회되어도 값을 적재하지 않는다.

```sql
SELECT s.CLASSIFICATIONID, s.CLASSSTRUCTUREID,
    c.CLASSSPECID, c.ASSETATTRID, a.DATATYPE, c.SECTION,
    c.MEASUREUNITID, c.DOMAINID, c.LINKEDTOATTRIBUTE, c.LINKEDTOSECTION,
    u.SEQUENCE, u.MANDATORY, u.USEINSPEC
FROM MAXIMO.CLASSSTRUCTURE s
JOIN MAXIMO.CLASSUSEWITH w
  ON w.CLASSSTRUCTUREID = s.CLASSSTRUCTUREID AND w.OBJECTNAME = 'ACTCI'
JOIN MAXIMO.CLASSSPEC c ON c.CLASSSTRUCTUREID = s.CLASSSTRUCTUREID
JOIN MAXIMO.ASSETATTRIBUTE a
  ON a.ASSETATTRIBUTEID = c.ASSETATTRIBUTEID AND a.ASSETATTRID = c.ASSETATTRID
JOIN MAXIMO.CLASSSPECUSEWITH u
  ON u.CLASSSPECID = c.CLASSSPECID AND u.OBJECTNAME = 'ACTCI'
WHERE s.CLASSIFICATIONID IN ('SYS.COMPUTERSYSTEM', 'SYS.VIRTUALCOMPUTERSYSTEM')
  AND c.ASSETATTRID IN (
    'COMPUTERSYSTEM_ARCHITECTURE',
    'COMPUTERSYSTEM_CPUSPEED',
    'COMPUTERSYSTEM_CPUTYPE',
    'COMPUTERSYSTEM_FQDN',
    'COMPUTERSYSTEM_MANAGEDSYSTEMNAME',
    'COMPUTERSYSTEM_MANUFACTURER',
    'COMPUTERSYSTEM_MEMORYSIZE',
    'COMPUTERSYSTEM_MODEL',
    'COMPUTERSYSTEM_NAME',
    'COMPUTERSYSTEM_NUMCPUS',
    'COMPUTERSYSTEM_PRIMARYMACADDRESS',
    'COMPUTERSYSTEM_SERIALNUMBER',
    'COMPUTERSYSTEM_SIGNATURE',
    'COMPUTERSYSTEM_SYSTEMBOARDUUID',
    'COMPUTERSYSTEM_TYPE',
    'COMPUTERSYSTEM_UUID',
    'COMPUTERSYSTEM_VIRTUAL',
    'COMPUTERSYSTEM_VMID',
    'COMPUTERSYSTEM_BIOSMANUFACTURER',
    'COMPUTERSYSTEM_ROMVERSION',
    'COMPUTERSYSTEM_BIOSRELEASEDATE',
    'COMPUTERSYSTEM_CPUCORESINSTALLED')
ORDER BY s.CLASSIFICATIONID, c.ASSETATTRID;
```

ACTCISPEC의 부모·템플릿 참조는 [공통 매핑](../actcispec.md)을 적용한다.
MEMORYSIZE·CPUSPEED의 MEASUREUNITID는 4절의 명시적 단위 매핑을 우선한다.
구현은 ACTCINUM 및 속성 키로 기존 ID를 조회한 뒤 UPDATE 또는 INSERT한다.
본체·스펙을 장비별 트랜잭션으로 묶으며 실제 저장 SQL은 ComputerCiIntegrate에 있다.

## 6. 검증과 남은 작업

두 D42 서버에서 5절 원천 SQL의 실행·장비 키 중복 여부와 원천 필드·단위·CPU/포트 보강을 확인했다.
Maximo에서 분류·속성 타입·적용 설정·단위 코드를 대조했다. 실제 업무 테이블 쓰기는 수행하지 않았다.
코드의 원천·정의 조회 SQL도 읽기 전용으로 확인했다. H2 Db2 모드에서 부모·템플릿 연결,
재실행·롤백·페이징·정의 누락을 테스트했으며 전체 테스트와 실행 JAR 빌드를 통과했다.

[ISSUE-11](../../../open-issues.md#issue-11-actual-ci-분류속성관계와-식별자-매핑)에서
추가 속성 등록, 미대응 속성, 실제 적재 검증과 후속 운영 정책을 추적한다.
[ISSUE-8](../../../open-issues.md#issue-8-actual-ci-대상-범위)은 후속 CI·관계와 수집 범위 확장을 다룬다.

관측 근거: [D42 원천](../../../knowledge/device42/computer-inventory.md),
[Maximo 분류·스펙](../../../knowledge/maximo/computer-classification-specs.md).
ROMVERSION의 BIOS 버전 의미는 [IBM CCMDB 가이드](https://www.redbooks.ibm.com/redbooks/pdfs/sg247879.pdf)에서,
TYPE의 ComputerSystem 값은 [IBM ComputerSystem 매핑](https://www.ibm.com/docs/en/tivoli-monitoring/6.3.0?topic=cdm-computersystem-class)에서 확인했다.
