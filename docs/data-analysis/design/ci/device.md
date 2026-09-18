# Device 기준 CI 통합 수집 설계

> 상태: 1차 Device 통합·Switch ACTCI 구현 완료, 2차 기준정보 추천 설계 완료,
> Network Cluster ACTCI·관계 조회 구현 완료, MAS UI 관계 규칙·실적재 미적용 · 2026-09-17.
> 근거: [D42 두 서버 원천 조사](../../knowledge/device42/device-ci-source.md), [Maximo 분류·스펙 조사](../../knowledge/maximo/device-ci-classifications.md).
> 실행 계획: [Device CI 통합 구현 계획](../../../superpowers/plans/2026-09-15-device-ci-integration.md).
> 현재 구현 정본은 [Device 매핑](../../data-mapping/ci/types/device.md)이다. 이번 1차 작업은 저장소 코드와 문서만 변경했으며 Maximo 기준정보·업무 데이터는 변경하지 않았다.
> 2차 MAS UI 입력안은 [Device CI 기준정보 설계](device-reference-data.md)를 따른다.

## 1. 결론과 합의된 방향

**Computer 수집기를 Device 수집기로 넓히고, 한 조회 결과에서 종류별 분류와 스펙을 선택하는 구성이 적합하다.**
기존 조인에 cluster 연결 집계를 더해 Computer·VM·물리 네트워크 장비를 함께 읽을 수 있다.
1차 지원 투영은 .68에서 36행(Computer/VM 34 + Switch 2), .35에서 72행(70 + 2)이었다.
Network Cluster 2개씩을 추가한 현재 Device 투영은 38 / 74행이며 각각 장비 PK도 같은 개수다.
물리 Printer는 계속 적재에서 제외한다.

사용자와 합의한 범위는 다음과 같다.

- 기존 본체·스펙 적재 구조를 유지하며 Computer 중심 조회를 Device 중심으로 확장한다.
- 이미 연결한 원천에서 유용한 정보를 최대한 확인한다. 정보 보강만을 위해 조인을 계속 추가하지 않는다.
- 관계는 CI 본체 적재 이후의 기존 별도 단계에서 키를 재조회한다. 본체 DTO를 관계 처리까지 쌓아두지 않는다.
- Switch는 `second_device_fk`로 연결된 단일 cluster의 단일 `fw_device_type=Switch`일 때만
  `SYS.GENERICSWITCH`로 분류한다. Router·누락·충돌은 적재하지 않고 진단 로그로 남긴다.
- Switch stack의 논리 객체인 cluster도 기존 `SYS.COMPUTERSYSTEMCLUSTER`로 별도 수집한다.
  물리 Switch와 합치지 않고 기존 `FEDERATES`에 `1:N` 분류 규칙을 추가해 연결한다.
- Printer는 기존 `SYS.PRINTER`를 본체 분류로 사용하지 않고, 물리 Printer 전용 ACTCI·CI 분류와 속성 세트를 사용한다.

**조회 통합과 분류 선정은 구분해야 한다.** `network_device=true`만으로는 부족하지만,
cluster 연결의 `fw_device_type`으로 관측 표본의 Switch를 명확히 판정한다. Router 표본과 판정값은
관측되지 않았으므로 지원하지 않는다. SYS.PRINTER는 모델·시리얼·RAM을 담는 장비 템플릿이 아니므로
프린터를 그 분류로 바로 보내지 않는다. 남은 결정의 정본은 [ISSUE-8·11](../../open-issues.md)이다.

## 2. 수집 범위와 분기 대조표

아래 분기는 1차 확정 범위와 후속 범위를 구분해 표현한다.

| D42 조건 | 수집 종류 | Maximo 대상 | 처리 제안 |
| --- | --- | --- | --- |
| 현행 COMPUTER 조건 중 `type='physical'` | Computer | SYS.COMPUTERSYSTEM | 기존 분류·식별자·변환 유지 |
| 현행 COMPUTER 조건 중 `type='virtual'` | Virtual Computer | SYS.VIRTUALCOMPUTERSYSTEM | 기존 VM_ID 조건까지 유지 |
| `type='physical' AND network_device=true`, Printer subtype 아님 | Network | `SYS.GENERICSWITCH` | 단일 연결 cluster·단일 `fw_device_type=Switch`일 때만 1차 적재 |
| 위 조건이 Router·NULL·복수 cluster·종류 충돌 | 미판별 Network | 없음 | 1차 적재 제외, 원천 진단값과 건수 로그 |
| `type='physical' AND physicalsubtype='Network Printer'` | Printer | `SYS.PHYSICALPRINTER` 제안 | 1차 적재 제외, MAS 기준정보 적용 후 활성화 |
| Network Printer subtype과 network 플래그 true가 겹침 | 판정 충돌 | 없음 | Printer 제외를 우선하고 로그로 남김 |
| `type='cluster' AND network_device=true AND fw_device_type='Switch'` | Network Cluster | `SYS.COMPUTERSYSTEMCLUSTER` | 구현 완료. 논리 스택 본체로 수집하고 물리 Switch와 `FEDERATES` 관계 생성 |
| Docker Container·unknown·PDU 및 나머지 | 이번 확장 대상 밖 | 없음 | 별도 관리 단위 결정 전 포함하지 않음 |

현행 COMPUTER 조건의 물리 subtype은 Generic·Rackable·Blade·WorkStation·ThinClient·Laptop,
가상 subtype은 Internal VM·Amazon EC2 Instance·VMWare·Hyper-V이며 network 플래그 false/NULL이다.
1차 제품 WHERE는 이 조건에 `type='physical' AND network_device=true`를 OR로 추가한다.
따라서 network 플래그가 false/NULL인 Printer는 아직 읽지 않는다. Count 쿼리와 페이지 조회에는
동일한 후보 조건을 사용한다.

장비 후보 판정은 `d.network_device`를 사용한다. 관측한 네트워크 장비에서 `h.network_device`는 모두 false였다.
Rackable/Generic과 모델명은 스위치·라우터 구별에 쓰지 않는다. `view_netport_v1.second_device_fk`로
연결한 cluster의 `details->>'fw_device_type'`만 종류 근거로 사용한다.

## 3. 기존 구조에서 바꿀 부분

```text
CiIntegrationJob
  ├─ DeviceCiImport
  │    ├─ DeviceCiQuery → DeviceSource
  │    ├─ DeviceCiMapper → 종류·분류·본체·스펙 선택 (CiSpecMapper 사용)
  │    └─ maximo.ci.ActCiWriter → 본체·스펙 저장
  ├─ 나머지 CI 수집기
  └─ CiRelationJob              기존 위치, 관계 키 별도 재조회
```

- 2026-09-18 구조 정리 후 조회·매핑·저장 책임을 위처럼 분리했다. Device 내부 종류마다 수집기·Writer·Mapper를 만들지는 않는다.
- 종류별 분류 선택과 스펙 메서드는 DeviceCiMapper 안에 둔다. 별도 전략 프레임워크는 도입하지 않는다.
- `ComputerSpec`의 공통 하드웨어 속성은 재사용할 수 있다. 새 분류 전용 속성이 실제로 확정되면 필요한 enum 항목만 추가한다.
- `CiClassification`에는 확정된 분류만 추가한다. 정의 캐시의 템플릿·자료형·단위 검증과 누락 스펙 처리 방식을 유지한다.
- **`MaximoSourcePolicy.CI_COMPUTER`를 전역 확대하지 않는다.** OS·Disk·Filesystem·기존 관계가 참조하므로 Device 전용 후보 조건을 별도로 둔다. 자식 CI 수집 범위 확대는 개별 매핑에서 판단한다.
- ACTCINUM은 `D42:DEVICE:<device_pk>`를 유지한다. 분류명을 키에 넣어 동일 장비를 새 CI로 만들지 않는다. 기존 키의 분류가 바뀌면 ACTCI와 재매핑된 ACTCISPEC의 분류·템플릿 참조를 갱신한다.
- 기존 Computer 수집기와 새 Device 수집기를 동시에 빈으로 등록하지 않는다. 같은 장비를 중복 처리하게 된다.

## 4. 필드별 원천 → 대상 대조표

### 4.1 기존 수집 필드와 공통 재사용

`d/h/v/b`는 Device / Hardware / 모델 제조사 Vendor / BIOS Vendor,
`cpu/pp`는 기존 CPU·기본 포트 집계다. 아래 대상 속성은 Computer와 GenericSwitch/Router에서 확인했다.
**프린터에는 본체 분류 결정 후 적용 여부를 다시 대조한다.**

| 수집 정보 | 기존 조인 범위의 Source | Target 또는 용도 | 적용 조건 |
| --- | --- | --- | --- |
| 장비 식별 | `d.device_pk` | ACTCI.ACTCINUM | `D42:DEVICE:` 접두 유지 |
| 이름 | `d.name` | ACTCI.ACTCINAME, COMPUTERSYSTEM_NAME | 기존 매핑 유지; SNMP sysName과 동일시하지 않음 |
| 설명 | `d.notes` | ACTCI.DESCRIPTION | 기존 매핑 유지; 이번 조사 결과에는 내용 대신 비NULL 여부만 보관 |
| 발견 시각 | `d.last_discovered` | ACTCI.LASTSCANDT | 기존 파싱·필수값 처리 유지; 수정 시각으로 임의 대체하지 않음 |
| 시리얼 | `d.serial_no` | COMPUTERSYSTEM_SERIALNUMBER | 장비 시리얼 |
| UUID | `d.uuid` | COMPUTERSYSTEM_UUID | 빈값이면 스펙 생략 |
| 모델 | `h.name` | COMPUTERSYSTEM_MODEL | LEFT JOIN 누락으로 본체 제외하지 않음 |
| 제조사 | `v.name` | COMPUTERSYSTEM_MANUFACTURER | 하드웨어 모델 제조사; CPU 제조사로 쓰지 않음 |
| RAM | `d.ram`, `d.ram_size_type` | COMPUTERSYSTEM_MEMORYSIZE | 기존 GB→GBYTE, MB→MBYTE 단위 대응; 원문 수치 유지 |
| CPU 수 | `d.total_cpus` | COMPUTERSYSTEM_NUMCPUS | CPU 파트 행 수·모델 수와 구분 |
| CPU 속도 | `d.cpu_speed`, `d.hz` | COMPUTERSYSTEM_CPUSPEED | 기존 GHz→GHZ, MHz→MHZ 대응 |
| CPU 모델 | `pm.name`, `pm.type_name='CPU'` | COMPUTERSYSTEM_CPUTYPE | 비어 있지 않은 모델 종류가 정확히 1개일 때 |
| CPU 아키텍처 | `p.details->>'architecture'` | COMPUTERSYSTEM_ARCHITECTURE | CPU 집계의 값 종류가 정확히 1개일 때; OS 아키텍처로 대체하지 않음 |
| CPU 총 코어 | `d.total_cpus * d.core_per_cpu` | COMPUTERSYSTEM_CPUCORESINSTALLED | 둘 다 있을 때 기존 계산 유지 |
| 대표 MAC | `pp.primary_mac` | COMPUTERSYSTEM_PRIMARYMACADDRESS | 장비에 직접 연결된 기본 포트가 정확히 1개일 때 |
| 시스템 유형 | 기존 상수 `ComputerSystem` | COMPUTERSYSTEM_TYPE | 기존 Computer 값 유지. 신규 분류의 값은 의미 확인 후 지정 |
| 가상 여부 | `d.type` | COMPUTERSYSTEM_VIRTUAL | 물리/가상 판정; 네트워크라는 이유로 가상으로 취급하지 않음 |
| VM 식별 | `d.vm_manager_int_id` | COMPUTERSYSTEM_VMID | Virtual Computer에만 적용 |
| BIOS 제조사 | `b.name` | COMPUTERSYSTEM_BIOSMANUFACTURER | BIOS Vendor 조인 |
| BIOS 버전 | `d.bios_version` | COMPUTERSYSTEM_ROMVERSION | BIOS revision·firmware revision과 구분 |
| BIOS 출시일 | `d.bios_release_date` | COMPUTERSYSTEM_BIOSRELEASEDATE | 기존 추가 속성 경로 유지; NUMERIC인 BIOSDATE에 문자열을 넣지 않음 |

### 4.2 같은 원천에서 추가로 확보할 정보

이 표는 **수집·매핑 후보 대조표**다. 대상 미정 필드를 모두 ACTCISPEC으로 적재하겠다는 뜻은 아니다.
DTO에는 의미가 확인된 원문 값·단위·연결 키를 담고, 빈 원천도 nullable 필드로 취급한다.
추가 Source 필드를 제품 DTO에 반영할 때 SQL alias와 읽기 타입을 명시한다.

| 추가 정보 | Source | 대상/사용 후보 | 이번 조사 결론 |
| --- | --- | --- | --- |
| 자산 번호 | `d.asset_no` | COMPUTERSYSTEM_ASSETTAG | 속성 존재. 업무 자산 번호와 tag 의미를 맞춘 뒤 추가 |
| 장비 분류 근거 | `d.type`, `physicalsubtype`, `virtualsubtype`, `network_device` | 분기·진단 | DTO의 원천 판별값으로 확보 |
| OS 식별·이름·버전 | `d.deviceos_fk`, `os_fk`, `os_name`, `os_version`, `os_version_no` | 기존 OS CI 또는 신규 스펙 검토 | Computer/Generic 템플릿에 OS 이름·버전 전용 속성 없음. 네트워크 OS 포함은 별도 범위 결정 |
| OS 아키텍처·지원 만료 | `d.os_architecture`, `os_support_expires` | OS CI 정보 | CPU 아키텍처·하드웨어 EOS와 구분; OS 수명주기 속성 문제는 ISSUE-11 |
| CPU당 코어·스레드 | `d.core_per_cpu`, `threads_per_core` | 자원 원문 | 총 코어 계산은 기존 사용. 세 분류에 스레드 전용 속성은 확인 안 됨 |
| CPU 파트 수량 | `p.pcount` 집계 | `d.total_cpus`와 품질 대조 | 기존 CPU 수를 자동 덮어쓰지 않음 |
| CPU 상세 | `p`의 슬롯·시리얼·펌웨어, `pm`의 코어·스레드·속도·단위 | 상세 보강 후보 | 여러 CPU 행의 값을 임의 MIN으로 묶지 않음. 현재 투영은 모델·아키텍처·행수·수량까지만 검증 |
| 디스크 요약 | `d.hard_disk_count`, `hard_disk_size`, `hard_disk_size_type` | 장비 요약 후보 | 독립 Disk CI를 대체하지 않음; 대상 속성·합산 의미는 미확정 |
| RAID 요약 | `d.hw_sw_raid`, `raid_type` | 장비 요약 후보 | 기존 Device 컬럼만 확보 가능; 신규 매핑 미확정 |
| BIOS 추가 버전 | `d.bios_revision`, `bios_fw_revision` | 펌웨어 보강 후보 | ROMVERSION과 같다고 가정하지 않음 |
| 모델 파트 번호 | `h.part_number` | 모델 상세 후보 | 양쪽 후보 범위 전건 빈값; 컬럼은 있음 |
| 모델 EOL·EOS | `h.end_of_life_date`, `end_of_support_date` | 사업 EOS 요구 | 전건 빈값이며 세 분류에 전용 속성 없음. 운영 원천·타겟 보완 필요 |
| 모델 크기·깊이·전력 | `h.size`, `depth`, `watts` | 하드웨어 상세 후보 | 원문 단위·모델 정격과 실측 의미를 확인한 후 매핑 |
| 모델 문서·제조사 홈페이지 | `h.specification_url`, `v.home_page` | 참고 URL 후보 | 현재 조인에서 조회 가능; 제품 필드 매핑 미확정 |
| 상태·운영 정보 | `d.in_service`, `state`, `impact`, `usage_type`, `service_level` | 상태/용도 후보 | D42 값과 Maximo 상태·도메인을 동일시하지 않음 |
| 위치 | `d.additional_location_info`, `datacenter`, 건물·실·랙 FK 및 배치 정보 | COMPUTERSYSTEM_LOCATIONTAG 등 후보 | 텍스트 의미 검토 필요. FK로 위치 이름을 얻으려면 별도 조인이므로 이번 범위 밖 |
| 생성·수정 시각 | `d.first_added`, `last_edited`, `last_changed` | 원천 진단 | 발견 시각 또는 ETL CHANGEDATE의 대체값으로 쓰지 않음 |
| 가상 호스트·관리자·섀시 | `d.virtual_host_device_fk`, `vm_manager_device_fk`, `host_chassis_device_fk` | 후속 관계 원천 | 본체에서 읽을 수 있으나 관계 단계에서 다시 조회 |
| VM 보조 정보 | `d.vm_manager_ref_id`, `vm_creation_date`, `virtual_host` | VM 상세 후보 | VM_ID와 혼합하지 않음 |
| 기본 포트 상세 | `n.port`, `name`, `port_speed`, `mtu` | 대표 포트 정보/Interface 후보 | 기본 포트 1개 조건에서만 투영; 전체 Interface 수집이 아님 |
| 포트 연결 키 | `n.remote_netport_fk`, `second_device_fk`, `module_device_fk`, `parent_part_fk`, `primary_vlan_fk` | 후속 관계 탐색 | 포트 수준 FK. 관계 조회는 전체 관련 포트에서 별도로 수행 |
| IP·태그·datastore 목록 | `d.ip_addresses`, `tags`, `datastores` | 다중값 정보 후보 | 컬럼 존재·비NULL만 확인. 주소 하나 또는 datastore 하나를 임의 선택하지 않음 |
| JSON 상세 | `d.details`의 hostname·DNS·firmware·power 등 | 발견 방식별 보강 후보 | 키 집합 조사 완료. 키별 의미·타입·값 유효성 검증 후 명시적으로 선택 |
| 사용자 정의 필드 | `d.vendor_custom_fields` | 필요 항목 선별 | 용도·담당자·태그 등 혼재. 전체 JSON 자동 스펙 적재는 미정 |
| SNMP SysName | 현재 직접 대응 원천 미확인 | SYS.SNMPSYSTEMGROUP의 SNMPSYSTEMGROUP_SYSNAME | 장비 이름으로 채우지 않음. 사업 요구 중 추가 조사 대상 |
| 프린터 트레이 | Part 뷰의 CPU 이외 타입 | 트레이 상세 후보 | 현재 CPU 필터 밖. 집계 범위 변경과 대상 정의가 필요 |

### 4.3 조회 시 유지할 제약

- Hardware·Vendor는 LEFT JOIN한다. CPU와 포트는 장비별로 먼저 집계해 Device당 한 행을 유지한다.
- 모델/아키텍처가 여러 종류이면 대표값을 비운다. 조사 SQL은 진단용 MIN과 count를 같이 반환하므로, 제품 SQL로 옮길 때 기존 `count=1` 가드를 보존한다.
- CPU 파트 상세·IP 배열처럼 다중값인 정보는 단일 스펙에 임의로 압축하지 않는다. 필요하면 별도 관리 단위나 명시적 집계 정책을 먼저 정한다.
- 새 조인을 추가하지 않아도 조회 컬럼·JSON 크기만큼 전송량은 늘어난다. 유용한 항목을 명시하고 `SELECT *`나 거대 JSON 전체를 제품 DTO에 무조건 포함하지 않는다.
- 조사 SQL의 `has_*`는 비NULL 여부다. 실제 본문 값·빈 JSON 여부·대표값 적합성을 검증한 결과로 해석하지 않는다.

## 5. 네트워크·프린터 선택안과 의견

### 네트워크

Switch 본체에는 `SYS.GENERICSWITCH`가 적합하다. Computer 98개 속성을 모두 갖고
전용 GENERICTYPE 속성 하나가 더 있어 공통 하드웨어 매핑을 재사용한다.
물리 Device의 플래그·subtype만으로는 종류를 고르지 않고, `second_device_fk`로 연결된 cluster가
정확히 하나이며 비어 있지 않은 `fw_device_type`도 정확히 한 종류인 `Switch`일 때만 확정한다.
양 서버 표본 네 대가 이 조건을 충족했다. Router·누락·충돌은 명시적으로 보류한다.

물리 스위치의 포트는 직접 소속이 아니라 cluster 쪽에 있다. 대표 MAC은
`second_device_fk=physical device_pk`인 cluster 포트의 비어 있지 않은 MAC 중 MIN을 사용한다.
관리 IP는 Cluster→IP `USES`로 직접 연결하고, 전체 Network Interface는 본체 스펙으로 압축하지 않고
후속 관계 범위로 남긴다.

cluster는 물리 Switch의 대체 레코드가 아니라 관리 IP와 포트를 소유하는 논리적 Network Cluster다.
기존 `SYS.COMPUTERSYSTEMCLUSTER`로 별도 ACTCI를 만들고, 상위 개념 관계는
`Network Cluster → FEDERATES → Network Device`로 표현한다. 현재 확인된 규칙은
`SYS.COMPUTERSYSTEMCLUSTER → SYS.GENERICSWITCH` `1:N`이며 상세 설정은
[기준정보 설계](device-reference-data.md#5-승격-범위와-관계)를 따른다.

SYS.APPLIANCE.NETWORKSYSTEM는 속성이 많지만 속성 연결과 ACTCI 적용 설정이 비어 있다.
설정 보완 없이 범용 네트워크 장비의 즉시 대체 분류로 사용하지 않는다.

### 프린터

| 선택안 | 장점 | 필요한 판단·작업 |
| --- | --- | --- |
| 기존 SYS.PRINTER로 바로 분류 | 기존 기능 분류 활용 | 현재 템플릿으로 모델·제조사·시리얼·RAM을 담지 못하므로 본체 수집 목적에 부적합 |
| 본체를 SYS.COMPUTERSYSTEM으로 수집하고 프린터 역할 구분 | 기존 하드웨어 속성 재사용 | 별도 프린터 분류 요구와 맞는지, 역할 표현 방법 결정 필요 |
| 프린터 장비용 분류·하드웨어 스펙 구성 | 종류별 분류와 장비 정보 수집을 함께 충족 | 기준정보·ACTCI 적용·CI 승격 설정 및 검증 필요 |

**세 번째 안을 선택한다.** 추천 분류는 `SYS.PHYSICALPRINTER`와 `CI.PHYSICALPRINTER`다.
기존 SYS.PRINTER 템플릿은 변경하지 않는다. 향후 프린터 기능 CI가 필요하더라도 별도 관리 목적·키·관계를 정한 뒤 도입한다.

`SYS.GENERICSWITCH`는 ACTCI 분류와 99개 속성 적용 설정이 있지만 CITEMPLATE 등록은 0건이다.
`CI.FCSWITCHFUNCTION`과 `CI.IPSTORAGESWITCHFUNCTION`은 일반 Ethernet Switch 본체에 맞지 않는다.
따라서 Switch Authorized CI는 `CI.GENERICSWITCH`를 새로 만들고 본체 1:1 승격 범위를 둔다.
Printer도 ACTCI·CI 물리 본체 분류를 신규 생성한다. 정확한 속성·순서·UI 등록 절차는
[기준정보 설계](device-reference-data.md)가 정본이다.

## 6. 구현 순서와 완료 기준

1. **1차 완료:** `ComputerCiIntegrate`·`ComputerSource`를 Device 구조로 교체하고 기존 Computer 필터는 자식 CI·관계용으로 유지한다.
2. **1차 완료:** Device 본체 후보, cluster 집계, Switch 판정·대표 MAC·분류 전용 GENERICTYPE을 구현한다. Router·Printer·미판별 Network는 제외한다.
3. **1차 완료:** 매핑 정본을 Device 문서로 이동하고 두 D42의 PK 유일성·Count/조회 일치와 자동 테스트를 검증한다.
4. **2차 완료:** Switch CI 대상 분류·승격 속성과 물리 Printer ACTCI·CI 분류·속성 세트·관계 범위를 설계했다. MAS UI에는 아직 적용하지 않았다.
5. **코드 완료:** Network Cluster 본체 매핑과 `FEDERATES` 관계 조회를 구현했다. 양 서버의 Device
   예상 건수는 38/74건이고 Cluster·Switch 관계는 각 2쌍이다. MAS UI 관계 규칙 등록 후 실제 적재,
   재실행 멱등성과 UI 토폴로지 검증이 남아 있다.
6. **3차 예정:** 기준정보 적용을 확인한 뒤 Printer 분기를 활성화하고 ACTCI 적재·CI 승격·UI·재실행 멱등성을 검증한다.

관계 확대는 후속 작업이다. 기존 OS→Computer·Computer→Disk·Computer→Filesystem 흐름을 유지하고,
Device 간 연결·Interface·IP는 관계 원천을 재조회하여 별도 설계한다.
본체 필터 확장으로 기존 관계 범위가 의도치 않게 넓어지지 않는지 확인한다.

## 7. 이번 작업에서 완료한 것

- 두 D42의 기존 조인 범위 투영·유형 분포·키 유일성·포트 소속·JSON 키 조사.
- Maximo 후보 분류·실제 스펙·적용 설정·관계·승격 범위 조회와 대조표 작성.
- `DeviceCiImport`·`DeviceSource` 구조 전환과 `SYS.GENERICSWITCH` ACTCI 분기 구현.
- 양 서버에서 36 / 72행, Switch 각 2대, 장비 PK 중복 0, cluster·종류 단일성과 대표 MAC 확인.
- H2 Db2 모드 자동 테스트로 기존 Computer·VM과 Switch·제외 분기를 검증.
- Switch CI 18개와 Printer ACTCI·CI 13개 속성, 본체 1:1 승격 범위, MAS UI 등록·검증·롤백 기준 설계.
- `SYS.COMPUTERSYSTEMCLUSTER` 본체 수집과 Cluster→Switch `FEDERATES` 관계 조회 구현. 현재 Device
  예상 38 / 74행, 관계 각 2쌍이며 실제 Maximo 적재는 UI 규칙 등록 후 검증한다.

제품 Java와 문서는 변경했지만 Maximo 타겟 데이터·기준정보는 변경하지 않았다. 조회 SQL 여섯 파일은
[탐색 쿼리 목록](../../exploration-queries/README.md)에 등록하며 원본 결과는 로컬 작업 폴더에만 보관한다.
