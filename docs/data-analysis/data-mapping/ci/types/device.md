# Device CI

> Target: MAXIMO.ACTCI · MAXIMO.ACTCISPEC
> 원천·메타데이터 확인: 2026-09-15 · D42 .68 / .35 · Maximo BLUDB
> 구현: DeviceCiIntegrate · 상태: Computer·VM·Switch 수집과 ACTCI·ACTCISPEC 저장 및 자동 테스트 완료. 실제 Maximo Switch 적재·CI 승격·UI 검증은 미완료.
> 실행 방법·추가 속성 등록·현재 처리 동작은 [실행 준비](device-run.md)를 따른다. 미대응 항목은 아래 표에 구분한다.

## 1. 대상과 식별자

물리 Computer, VM과 판정 가능한 물리 Switch를 장비당 하나의 ACTCI로 수집한다.
이번 범위는 본체와 요약 스펙이며 Printer와 Router는 아직 적재하지 않는다.
개별 CPU·Disk·OS·Interface 등의 CI와 관계는 후속 유형에서 정의한다.
DPA 적재 결과나 변환 규칙에 의존하지 않는다.

| 항목 | 규칙 |
| --- | --- |
| 물리 분류 | `SYS.COMPUTERSYSTEM` |
| VM 분류 | `SYS.VIRTUALCOMPUTERSYSTEM` |
| Switch 분류 | `SYS.GENERICSWITCH` |
| 원천 키 / ACTCINUM | `D42:DEVICE:<device_pk>` |
| ACTCIID | Maximo 숫자 채번. 원천 PK를 대입하지 않음 |
| 갱신 | 같은 ACTCINUM은 기존 ACTCIID 유지. device_pk가 바뀌면 신규 CI |
| 스펙 참조 | ACTCINUM·CLASSSTRUCTUREID는 본체와 동일, REFOBJECTID=ACTCIID |
| 스펙 선택 | Computer 공통 18개와 Switch의 GENERICTYPE. BIOS·CPU 코어 수 추가 경로 유지 |

CiClassification에 사용할 분류명을 명시하고, CI 실행 시작 시 공통 캐시로 CLASSSTRUCTUREID를 조회한다.
환경별 숫자·문자열 ID를 상수로 고정하지 않는다. ComputerSpec에는 전체 ASSETATTRID를 명시한다.
CI 분류의 CLASSSPECID를 ACTCISPEC에 복사하지 않는다. Switch 판정은 물리 장비와
cluster의 포트 수준 연결을 따라 얻은 단일 `fw_device_type=Switch`에만 허용한다.

## 2. 원천과 조회 조건

| Source | 용도 | 연결 |
| --- | --- | --- |
| `view_device_v2 d` | 본체·메모리·CPU 요약·BIOS·VM ID | device_pk가 수집 단위 |
| `view_hardware_v2 h` | 모델 | h.hardware_pk=d.hardware_fk |
| `view_vendor_v1 v / b` | 장비 / BIOS 제조사 | v.vendor_pk=h.vendor_fk / b.vendor_pk=d.bios_vendor_fk |
| `view_part_v1 p` + `view_partmodel_v1 pm` | CPU 모델·아키텍처 | p.device_fk=d.device_pk, pm.partmodel_pk=p.partmodel_fk, pm.type_name='CPU' |
| `view_netport_v1 n` | Computer 기본 포트와 Switch cluster 연결·대표 MAC | 기본 포트 직접 연결 또는 n.second_device_fk=물리 device_pk |
| `view_device_v2 c` | Switch 종류 | c.device_pk=n.device_fk, c.type='cluster' |

보강 정보는 LEFT JOIN한다. CPU·포트는 먼저 장비별로 집계하여 본체 행을 늘리지 않는다.

이번 매핑은 다음 조건을 수집 후보로 명시한다. D42 전체 유형의 목록은 아니다.

| 조건 | 포함 값 |
| --- | --- |
| 공통 | type이 physical / virtual이고 network_device가 false 또는 NULL |
| physical | Generic, Rackable, Blade, WorkStation, ThinClient, Laptop |
| virtual | Internal VM, Amazon EC2 Instance, VMWare, Hyper-V |
| Switch 후보 | `type='physical' AND network_device=true` |

Switch 후보는 `second_device_fk`로 연결된 cluster가 정확히 하나이고, 연결 cluster의
비어 있지 않은 `details->>'fw_device_type'` 종류도 정확히 하나이며 그 값이 `Switch`일 때만
`SYS.GENERICSWITCH`로 매핑한다. Router·종류 누락·복수 cluster·종류 충돌은 조회하되 매핑에서
제외하고 로그를 남긴다. Printer·설비·컨테이너·cluster·unknown은 이 범위에 포함되지 않는다.
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
| CLASSSTRUCTUREID | 분류 | 변환 | d.type·network_device·network_kind → 분류명 | 1절의 ACTCI 적용 분류 조회 |
| DESCRIPTION | 설명 | 직접 | d.notes | 원문; 상세 설명으로 자동 분리하지 않음 |
| LASTSCANDT | 최종 발견 시각 | 변환 | d.last_discovered | JVM 기본 시간대로 변환. 파싱 실패는 해당 Device 생략, 누락은 NULL 전달 |
| HASLD | 상세 설명 있음 | 상수 | — | 상세 설명 미사용 시 0 |
| CHANGEBY | 변경자 | 상수 | `Device42` | 연계 식별 문자열. Maximo 사용자 계정 검증은 하지 않음 |
| CHANGEDATE | 변경 날짜 | 변환 | 매핑 시각 | JVM 기본 시간대. 같은 배치의 본체·스펙이 같은 시각 사용 |
| LANGCODE | 언어 코드 | 상수 | `KO` | 코드 상수. 원천 이름의 언어로 추정하지 않음 |
| GUID / CCIDISGUID | 발견 ID / 통합 ID | 미결 | 직접 대응 미확정 | 신규는 미설정, 기존 값은 유지. d.uuid는 UUID 스펙에 대응 |
| EXTENDEDINSTANCES | 확장 인스턴스 | 원천없음 | — | 이번 매핑에서 사용하지 않음 |
| PLUSPCUSTOMER | 기본 고객 | 원천없음 | — | D42 customer_fk와 Maximo 고객 코드 대응 없음 |

## 4. ACTCISPEC 값 매핑

적용 분류는 두 ACTCI Computer 분류와 `SYS.GENERICSWITCH`다. 아래 ASSETATTRID는
공통 접두어 `COMPUTERSYSTEM_`를 생략했다.
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
| PRIMARYMACADDRESS | 대표 MAC | ALNVALUE | 변환 | n.hwaddress | Computer는 기본 포트가 정확히 1개일 때, Switch는 cluster 포트 중 second_device_fk가 물리 장비를 가리키는 MAC의 MIN. 두 경로를 혼합하지 않음 |
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
| BIOSRELEASEDATE | BIOS 출시일 원문 | ALNVALUE | 직접 | d.bios_release_date | 원문 보존. 전역 속성·CI 분류 등록됨, ACTCI 템플릿 미등록. 아래 참조 |
| GENERICCOMPUTERSYSTEM_GENERICTYPE | Generic 장비 종류 | ALNVALUE | 변환 | network_kind | `SYS.GENERICSWITCH`에만 단일 판정값 `Switch` 적재 |

코어 수는 D42의 “CPU 수 × CPU당 코어 수”로 계산한 해당 장비의 보고 총량이다.
VM에서는 VM에 보고된 구성으로 해석하며 호스트의 물리 코어 수나 활성 코어 수를 뜻하지 않는다.
CPU 파트 수·모델별 cores 합계로 대체하지 않는다.

**단위:** MEMORYSIZE·CPUSPEED의 ACTCI 템플릿 단위는 현재 공란이다.
위 단위 코드를 ACTCISPEC.MEASUREUNITID에 명시하고 수치는 환산하지 않는 것이 이번 매핑 규칙이다.
GBYTE·MBYTE·GHZ·MHZ는 Maximo에 등록되어 있다. 단위 누락·미지원 표기에는 임의 단위를 붙이지 않는다.
IBM CDM 원래 단위와 동일하다고 가정하지 않으며 승격·후속 연계도 값과 단위를 함께 처리해야 한다.

**추가 속성 등록 상태:** `COMPUTERSYSTEM_BIOSRELEASEDATE`는 2026-09-15 사용자가 수동 등록했다.
전역 `ASSETATTRIBUTE`(ASSETATTRIBUTEID 3065338, ALN, ORGID·SITEID 없음)와
`CI.COMPUTERSYSTEM`의 `CLASSSPEC`(CLASSSPECID 19395931, OBJECTNAME='CI', SEQUENCE 2)이 있다.
**ACTCI 쪽 `SYS.COMPUTERSYSTEM`에는 아직 템플릿이 없다.**
따라서 적재는 계속 명시적 추가 속성 경로를 쓰며 `CLASSSPECID=NULL`, `DISPLAYSEQUENCE=180`이다.
ACTCI 템플릿을 등록하면 기존 경로가 자동으로 우선한다.
ASSETATTRIBUTE에 전역 ALN 정의가 정확히 한 건 있어야 사용한다. 분류 템플릿이 없으면
명시적 추가 속성으로 CLASSSPECID=NULL, DISPLAYSEQUENCE=180, MANDATORY=0을 사용한다.
SECTION·LINKEDTOATTRIBUTE·LINKEDTOSECTION은 NULL, 단위는 속성 정의를 따른다.
기존 템플릿이 있으면 그 설정을 우선하며, 잘못된 적용 설정을 추가 경로로 우회하지 않는다.
다른 분류의 템플릿 ID를 빌려오지 않는다. 템플릿 없는 속성의 실제 UI·승격은 미검증이다.
기존 `COMPUTERSYSTEM_BIOSDATE`는 NUMERIC이고 날짜 인코딩 규약은 확인되지 않아 사용하지 않는다.
이는 ACTCISPEC 테이블의 새 컬럼이 아니라 속성 정의와 값 행을 추가하는 작업이다.

## 5. 조회 SQL

### D42 원천

아래 SQL은 수집·매핑용 SELECT이며 미결 항목의 값을 생성하지 않는다.
구현은 같은 후보 조건으로 COUNT를 조회한 뒤 아래 SQL에 LIMIT·OFFSET을 붙여 1,000건씩 처리한다.
`model_count`·`arch_count`·`default_port_count`와 network 진단값은 복수 값 또는 미판정 때문에
보강·본체 매핑을 생략했는지 구분하며 스펙으로 적재하지 않는다.

Count SQL:

```sql
SELECT COUNT(*)
FROM view_device_v2 d
WHERE (
    d.type IN ('physical', 'virtual')
    AND (d.network_device = false OR d.network_device IS NULL)
    AND (
        (d.type = 'physical' AND d.physicalsubtype IN
            ('Generic', 'Rackable', 'Blade', 'WorkStation', 'ThinClient', 'Laptop'))
        OR
        (d.type = 'virtual' AND d.virtualsubtype IN
            ('Internal VM', 'Amazon EC2 Instance', 'VMWare', 'Hyper-V'))
    )
)
OR (d.type = 'physical' AND d.network_device = true);
```

페이지 SQL:

```sql
WITH device AS (
    SELECT d.*
    FROM view_device_v2 d
    WHERE (
        d.type IN ('physical', 'virtual')
        AND (d.network_device = false OR d.network_device IS NULL)
        AND (
            (d.type = 'physical' AND d.physicalsubtype IN
                ('Generic', 'Rackable', 'Blade', 'WorkStation', 'ThinClient', 'Laptop'))
            OR
            (d.type = 'virtual' AND d.virtualsubtype IN
                ('Internal VM', 'Amazon EC2 Instance', 'VMWare', 'Hyper-V'))
        )
    )
    OR (d.type = 'physical' AND d.network_device = true)
), cpu AS (
    SELECT p.device_fk,
        COUNT(DISTINCT NULLIF(TRIM(pm.name), '')) AS model_count,
        MIN(NULLIF(TRIM(pm.name), '')) AS cpu_model,
        COUNT(DISTINCT NULLIF(TRIM(p.details->>'architecture'), '')) AS arch_count,
        MIN(NULLIF(TRIM(p.details->>'architecture'), '')) AS architecture
    FROM view_part_v1 p
    JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
    JOIN device d ON d.device_pk = p.device_fk
    WHERE pm.type_name = 'CPU'
    GROUP BY p.device_fk
), primary_port AS (
    SELECT n.device_fk, COUNT(*) AS default_port_count,
        MIN(NULLIF(TRIM(n.hwaddress), '')) AS primary_mac
    FROM view_netport_v1 n
    JOIN device d ON d.device_pk = n.device_fk
    WHERE n.is_default = true
    GROUP BY n.device_fk
), network_info AS (
    SELECT n.second_device_fk AS physical_pk,
        CASE WHEN COUNT(DISTINCT n.device_fk) = 1 THEN MIN(n.device_fk) END AS cluster_pk,
        COUNT(DISTINCT n.device_fk) AS cluster_count,
        COUNT(DISTINCT NULLIF(TRIM(c.details->>'fw_device_type'), '')) AS network_kind_count,
        MIN(NULLIF(TRIM(c.details->>'fw_device_type'), '')) AS network_kind,
        MIN(NULLIF(TRIM(n.hwaddress), '')) AS network_mac
    FROM view_netport_v1 n
    JOIN device d ON d.device_pk = n.second_device_fk
    JOIN view_device_v2 c ON c.device_pk = n.device_fk
    WHERE d.type = 'physical' AND d.network_device = true
      AND c.type = 'cluster' AND c.network_device = true
    GROUP BY n.second_device_fk
)
SELECT d.device_pk, d.type, d.physicalsubtype, d.network_device,
    ni.cluster_pk, ni.network_kind, ni.network_kind_count, ni.cluster_count,
    'D42:DEVICE:' || CAST(d.device_pk AS varchar) AS source_id,
    d.name, d.notes, d.serial_no, d.uuid, d.last_discovered,
    h.name AS model, v.name AS manufacturer,
    d.ram, d.ram_size_type, d.total_cpus, d.core_per_cpu,
    CAST(d.total_cpus AS bigint) * d.core_per_cpu AS total_cores,
    d.cpu_speed, d.hz AS cpu_speed_unit,
    CASE WHEN cpu.model_count = 1 THEN cpu.cpu_model END AS cpu_type,
    CASE WHEN cpu.arch_count = 1 THEN cpu.architecture END AS architecture,
    CASE WHEN d.network_device = true THEN ni.network_mac
         WHEN pp.default_port_count = 1 THEN pp.primary_mac END AS primary_mac,
    'ComputerSystem' AS system_type,
    CASE d.type WHEN 'virtual' THEN 'true' ELSE 'false' END AS is_virtual,
    CASE WHEN d.type = 'virtual' THEN d.vm_manager_int_id END AS vm_id,
    b.name AS bios_manufacturer, d.bios_version, d.bios_release_date,
    cpu.model_count, cpu.arch_count, pp.default_port_count
FROM device d
LEFT JOIN view_hardware_v2 h ON h.hardware_pk = d.hardware_fk
LEFT JOIN view_vendor_v1 v ON v.vendor_pk = h.vendor_fk
LEFT JOIN view_vendor_v1 b ON b.vendor_pk = d.bios_vendor_fk
LEFT JOIN cpu ON cpu.device_fk = d.device_pk
LEFT JOIN primary_port pp ON pp.device_fk = d.device_pk
LEFT JOIN network_info ni ON ni.physical_pk = d.device_pk
ORDER BY d.device_pk
LIMIT ? OFFSET ?;
```

### Maximo 공통 캐시 조회

CiDefinitionLoader가 CI 실행 시작 시 아래 정의를 조회한다. 실제 코드의 분류명 IN 목록은
CiClassification.values()에서 생성하고, 스펙은 앞에서 조회한 분류 ID 목록을 바인딩한다.
아래 SQL은 현재 Device 본체 enum의 세 분류로 재조회할 수 있는 형태다.

```sql
SELECT s.CLASSIFICATIONID,s.CLASSSTRUCTUREID
FROM MAXIMO.CLASSSTRUCTURE s
WHERE s.CLASSIFICATIONID IN
    ('SYS.COMPUTERSYSTEM','SYS.VIRTUALCOMPUTERSYSTEM','SYS.GENERICSWITCH')
  AND EXISTS (
      SELECT 1 FROM MAXIMO.CLASSUSEWITH u
      WHERE u.CLASSSTRUCTUREID=s.CLASSSTRUCTUREID AND u.OBJECTNAME='ACTCI'
  );
```

ASSETATTRIBUTE는 이름 중복을 보존하도록 숫자 ASSETATTRIBUTEID로 캐싱한다.

```sql
SELECT ASSETATTRIBUTEID,ASSETATTRID,DATATYPE,MEASUREUNITID,ORGID,SITEID
FROM MAXIMO.ASSETATTRIBUTE;
```

설정이 없는 템플릿도 조회하여 존재 여부를 기록한다. 정상 템플릿에는 속성 ID·이름의 일치와
ACTCI 적용 설정·표시 순서·필수 여부가 필요하다. 비정상 템플릿을 추가 경로로 우회하지 않는다.

```sql
SELECT c.CLASSSTRUCTUREID,c.CLASSSPECID,c.ASSETATTRID,c.ASSETATTRIBUTEID,
    c.SECTION,c.MEASUREUNITID,c.LINKEDTOATTRIBUTE,c.LINKEDTOSECTION,
    u.SEQUENCE,u.MANDATORY
FROM MAXIMO.CLASSSPEC c
LEFT JOIN MAXIMO.CLASSSPECUSEWITH u
  ON u.CLASSSPECID=c.CLASSSPECID AND u.OBJECTNAME='ACTCI' AND u.USEINSPEC=1
  AND u.CLASSSTRUCTUREID=c.CLASSSTRUCTUREID AND u.ASSETATTRID=c.ASSETATTRID
  AND (u.SECTION=c.SECTION OR (u.SECTION IS NULL AND c.SECTION IS NULL))
WHERE c.CLASSSTRUCTUREID IN (SELECT s.CLASSSTRUCTUREID FROM MAXIMO.CLASSSTRUCTURE s
    WHERE s.CLASSIFICATIONID IN
        ('SYS.COMPUTERSYSTEM','SYS.VIRTUALCOMPUTERSYSTEM','SYS.GENERICSWITCH')
      AND EXISTS (SELECT 1 FROM MAXIMO.CLASSUSEWITH w
          WHERE w.CLASSSTRUCTUREID=s.CLASSSTRUCTUREID AND w.OBJECTNAME='ACTCI'));
```

ACTCISPEC의 부모·템플릿 참조는 [공통 매핑](../actcispec.md)을 적용한다.
MEMORYSIZE·CPUSPEED의 MEASUREUNITID는 4절의 명시적 단위 매핑을 우선한다.
구현은 ACTCINUM 및 속성 키로 기존 ID를 조회한 뒤 UPDATE 또는 INSERT한다.
mapData에서 본체·스펙 DTO를 만들고 putData에서 본체 ID를 확보한 뒤 스펙을 저장한다.
명시적 트랜잭션·롤백은 적용하지 않으며 실제 저장 SQL은 DeviceCiIntegrate와 공통 Writer의 상수로 분리한다.

## 6. 검증과 남은 작업

두 D42 서버에서 5절 원천 SQL을 2026-09-15 재실행했다. `.68`은 36행/고유 PK 36개,
`.35`는 72행/고유 PK 72개다. 서버별 Switch 2대는 cluster 1개와 단일
`network_kind=Switch`를 가지며 대표 MAC도 확보됐다. 기존 Computer·VM 조건은 변경하지 않았다.
기존 Computer·VM 34 / 70행의 공통 반환 필드는 확장 전 저장 결과와 행 단위로 동일했다.
Maximo에서 분류·속성 타입·적용 설정·단위 코드를 대조했다. 실제 업무 테이블 쓰기는 수행하지 않았다.
코드의 원천·정의 조회 SQL도 읽기 전용으로 확인했다. H2 Db2 모드에서 부모·템플릿 연결,
재실행·페이징·정의 누락·건별 오류 후 계속 처리와 Switch 정상 판정,
Router·종류 누락·cluster 충돌·Printer 제외를 테스트했다. 현재 검증 상태는 [실행 준비](device-run.md#검증)를 따른다.

[ISSUE-11](../../../open-issues.md#issue-11-actual-ci-분류속성관계와-식별자-매핑)에서
추가 속성 등록, 미대응 속성, 실제 적재 검증과 후속 운영 정책을 추적한다.
[ISSUE-8](../../../open-issues.md#issue-8-actual-ci-대상-범위)은 후속 CI·관계와 수집 범위 확장을 다룬다.

관측 근거: [D42 원천](../../../knowledge/device42/computer-inventory.md),
[Maximo 분류·스펙](../../../knowledge/maximo/computer-classification-specs.md).
ROMVERSION의 BIOS 버전 의미는 [IBM CCMDB 가이드](https://www.redbooks.ibm.com/redbooks/pdfs/sg247879.pdf)에서,
TYPE의 ComputerSystem 값은 [IBM ComputerSystem 매핑](https://www.ibm.com/docs/en/tivoli-monitoring/6.3.0?topic=cdm-computersystem-class)에서 확인했다.

## 7. 관계 매핑 — 2026-09-15

관측·선택 근거는 [관계 설계](../../../design/ci/relations.md),
Target 컬럼과 저장 SQL 제안은 [ACTCIRELATION](../actcirelation.md)을 참조한다.
2026-09-15 `./run.sh ci-relation`으로 운영 적재를 검증했다. COMPUTER_CONTAINS_DISK 19건,
COMPUTER_CONTAINS_FILESYSTEM 60건 모두 조회=적재이고, 재실행에서 `ACTCIRELATIONID`가
유지됐다(멱등성). Filesystem의 `device_fks` 배열 팬아웃(마운트포인트 하나에 복수 장비)은
이번 적재분에서 0건 관측됐고, 원천(D42 .35, 수집 필터 적용) 재조회로 원인을 확인했다 —
`pair_cnt=mountpoint_cnt=60`으로 현재 원천에 마운트포인트당 Computer가 둘 이상 붙은 사례가
없다. 적재 결함은 아니지만 `ANY(m.device_fks)`로 배열을 펼치는 경로 자체는 이 데이터로
실행되지 않아 미검증이다. 관계는 본체 task 안이 아니라
모든 CI 본체 적재가 끝난 뒤의 [관계 단계](../../../design/ci/relations.md#실행-위치--ci-본체-적재-이후-별도-단계)에서 저장한다.

| 의미 | SOURCECI | TARGETCI | RELATIONNUM | 조회 정의 소유 |
| --- | --- | --- | --- | --- |
| 디스크 포함 | D42:DEVICE:<device_fk> | D42:PART:<part_pk> | RELATION.CONTAINS | Disk 도메인 |
| 파일시스템 포함 | D42:DEVICE:<각 device_fks 원소> | D42:MOUNTPOINT:<mountpoint_pk> | RELATION.CONTAINS | Filesystem 도메인 |

출발 분류는 SYS.COMPUTERSYSTEM 또는 SYS.VIRTUALCOMPUTERSYSTEM,
도착 분류는 각각 DEV.DISKDRIVE, SYS.FILESYSTEM이다.
두 분류 쌍 모두 CONTAINMENT=1, REVRELATIONSHIP=0, CARDINALITY=1:N이다.
관계의 방향이 Computer 출발이어도 원천 연결 키를 아는 쪽, 즉 Disk·Filesystem 도메인의
조회 정의가 소유한다. 이 문서는 매핑 정본이고 조회 책임은 코드 쪽 기준이다.

아래 SELECT는 두 D42 서버에서 실행 검증했으며 관계 단계의 조회 정의가 그대로 쓴다.
관계 쌍에는 본체 전용 DISTINCT ON을 적용하지 않는다.

```sql
WITH computer AS (SELECT d.* FROM view_device_v2 d WHERE d.type IN ('physical','virtual')
AND (d.network_device=false OR d.network_device IS NULL)
AND ((d.type='physical' AND d.physicalsubtype IN ('Generic','Rackable','Blade','WorkStation','ThinClient','Laptop'))
OR (d.type='virtual' AND d.virtualsubtype IN ('Internal VM','Amazon EC2 Instance','VMWare','Hyper-V'))))
SELECT DISTINCT 'D42:DEVICE:' || CAST(c.device_pk AS varchar) AS sourceci,
       'D42:PART:' || CAST(p.part_pk AS varchar) AS targetci,
       'RELATION.CONTAINS' AS relationnum
FROM view_part_v1 p
JOIN view_partmodel_v1 pm ON pm.partmodel_pk=p.partmodel_fk
JOIN computer c ON c.device_pk=p.device_fk
WHERE pm.type_name='Hard Disk'
UNION ALL
SELECT DISTINCT 'D42:DEVICE:' || CAST(c.device_pk AS varchar),
       'D42:MOUNTPOINT:' || CAST(m.mountpoint_pk AS varchar),
       'RELATION.CONTAINS'
FROM view_mountpoint_v2 m
JOIN computer c ON c.device_pk=ANY(m.device_fks)
WHERE (m.fstype_name IS NULL OR m.fstype_name NOT IN ('overlay','devtmpfs','squashfs','efivarfs'))
ORDER BY sourceci,targetci;
```

VM–호스트는 virtual_host_device_fk로 양 끝이 확인된다.
토폴로지 의미 방향은 Host → VM, 코드 후보는 RELATION.VIRTUALIZES다.
원천 FK는 반대로 VM → Host를 가리킨다. 기존 Maximo 규칙도
`SYS.VIRTUALCOMPUTERSYSTEM → C`, `SWAPPED=1`이므로 물리 저장 순서는 실제 적재·UI 검증 후 확정한다.
실제 호스트는 physical과 virtual 모두 있다. 관계 단계가 본체 적재 이후에 실행되므로
뒤쪽 배치의 호스트를 놓치는 문제는 발생하지 않는다.
현재 1:1 규칙·SWAPPED·표시 검증은 남았으므로 위 우선 구현 표에 포함하지 않았다.
host_chassis_device_fk, vm_manager_device_fk를 같은 관계로 대체하지 않는다.
