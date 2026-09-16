# Computer CI 수집 설계

후속 확장안(2026-09-15): [Device 통합 수집 설계](device.md). 기존 조인에서 네트워크·프린터까지 조회하고 분류별 스펙을 선택하는 안과 필드 대조표를 담았다.

> 상태: Device 통합 전 Computer 설계 기록 · 갱신 2026-09-15. 현재 본체·스펙 매핑 정본은 [Device 매핑](../../data-mapping/ci/types/device.md)이다. 아래 초기 대조표는 후속 CI 구성 검토 자료다.

**합의된 원칙:** Computer부터 카테고리별로 조사·검증 후 확장한다. CI 매핑은 D42 원천을
기준으로 독립 정의하며 DPA 테이블·적재 결과·변환 규칙에 의존하지 않는다.
원천 구조는 [Computer 연관 수집 원천](../../knowledge/device42/computer-inventory.md),
실제 분류·스펙·관계 설정은 [분류 조사 결과](../../knowledge/maximo/computer-classification-specs.md),
진행 상태는 [Device](../../data-mapping/ci/types/device.md)를 참조한다.

**아래는 조사에 근거한 수집 추천안이다.** 추천과 확정 매핑을 구분한다.
분류가 존재하거나 현재 값이 비어 있다는 이유만으로 독립 CI 포함·제외를 결정하지 않는다.
사용자 화면의 CI.*·GENCSYS는 CI용이다. 현행 ACTCI 연계의 추천안은 ACTCI용 분류를 사용하며
CI/CISPEC으로 적재 대상을 변경하거나 두 계열 사이의 자동 승격을 구현한 것은 아니다.

## 전체 구성과 진행 범위

사용자와 정리한 Computer 중심 구성이다.

```text
Computer CI
├─ Computer 스펙
│  ├─ 이름·식별자
│  ├─ BIOS
│  ├─ 메모리 총량
│  └─ CPU 요약
├─ CPU CI
├─ Disk CI
├─ Filesystem CI
├─ OS CI
├─ Network Interface CI
├─ IP CI
└─ Software Installation CI
```

관련 정보의 구성도이며 모든 가지를 Computer와 직접 CONTAINS로 연결한다는 뜻은 아니다.
OS·설치 SW·IP의 실제 경로·코드·방향은 관계 설계에서 정한다.
먼저 **Computer 본체(ACTCI)와 Computer 스펙(ACTCISPEC)**의 규칙을 정하고 검증한 뒤,
연관 CI와 관계를 하나씩 추가한다.

## Computer 본체·스펙의 1차 적재 규칙 제안

합의된 항목은 아래 표에 명시한다. 나머지는 사용자가 검토할 제안이다.

| 정할 항목 | 추천안 | 확인·결정할 부분 |
| --- | --- | --- |
| 대상·분류 | 합의: 물리 Computer는 SYS.COMPUTERSYSTEM, VM은 SYS.VIRTUALCOMPUTERSYSTEM | 수집 서브타입·필터는 매핑 문서에 작성하고 두 서버에서 실행 확인 |
| 1차 스펙 | 합의: CI.COMPUTERSYSTEM 기준에 BIOS·CPU 코어 수 추가. **2026-09-15 BIOSRELEASEDATE가 CI.COMPUTERSYSTEM에 등록되어 기준이 19개가 됐다** | 항목별 원천 대응·미대응은 매핑 문서가 정본. CI 기준 대조 갱신 필요 |
| 동일 CI 판정 | 합의: 같은 원천 ID는 갱신, 원천 ID가 바뀌면 새 CI로 취급. 복합 원천 키는 ACTCINUM에 저장 | 이름·UUID·시리얼이 같아도 다른 원천 ID를 기존 CI로 자동 통합하지 않음. 숫자 PK의 시퀀스 예약 구현은 별도 확인 |
| 값·단위 | 원천 수치와 단위를 함께 보존하는 매핑 작성 | 단위별 코드는 매핑 문서 참조. UI·승격 후 값과 단위 전달은 검증 필요 |
| 빈 값·오류 | 정상적으로 확인된 빈 값은 빈 상태로 반영. 신규 빈 스펙은 생성하지 않고 기존 ETL 관리 스펙의 값은 비움 | 원천 조회 실패·변환 실패는 빈 값과 구분해 기존 정상값 보존. 명시적 삭제와 수집 미완료를 구분할 수 없는 원천은 보존·기록할지 결정 |
| 발견 시각·필수값 | LASTSCANDT는 device.last_discovered 사용. 없으면 해당 Computer 적재를 보류·집계하는 안 | 실행 시각을 발견 시각으로 대체하지 않음. 현재 LANGCODE·CHANGEBY 상수와 시간대 처리는 매핑 문서 참조. HASLD는 상세 설명 미사용 시 0 |
| 재실행·변경·원천 소실 | 동일 CI·스펙은 갱신하며, 이번 조회에 없는 Computer를 자동 삭제하지 않음 | 같은 분류에서는 스펙의 속성·섹션 키로 갱신. 분류가 바뀌면 ETL이 관리하는 이전 분류 스펙의 정리 규칙 필요. 수동 관리값은 덮어쓰지 않도록 소유 범위 정의 |
| 저장·검증 단위 | 합의: getData → mapData → putData. 명시적 트랜잭션·롤백은 후속 검토 | 본체 실패는 해당 Computer, 스펙 실패는 해당 속성을 건너뛰고 다음 데이터 처리. 현재 동작은 실행 준비 문서 참조 |

### 사용자 선택 전에 구현 측에서 확인할 내용

- CPU·BIOS·단위: 원천·Target 재대조를 수행했고 현재 대응은 매핑 문서에 있다. 추가 속성 등록·UI 표시·승격 전달을 검증한다.
- 채번: ACTCIID·ACTCISPECID의 Maximo 시퀀스 예약 방식, ACTCINUM 생성·원천 키 재조회 방법을 구체화한다. 원천 숫자 PK를 Target 숫자 PK에 직접 대입하지 않는다.
- 소유 범위: 어떤 Computer·스펙이 이 ETL에서 생성·관리됐는지 식별할 방법을 정한다. 원천 조회 성공 여부와 필드 누락을 구분할 수 있는지도 확인한다.

관리 단위·범위의 미결 상태는 ISSUE-8, 식별자·값·단위·저장 규칙의 미결 상태는 ISSUE-11에서 추적한다.

## CI 갱신 추적 키 — 확정

2026-09-14 사용자 합의. 운영 D42 한 서버 기준이다. 원천 ID 변경을 새 CI로 취급하는 원칙과,
서로 다른 원천 종류에서 같은 숫자 ID를 사용하는 문제를 구분한다.

- ACTCIID는 모든 종류의 CI가 공유하는 숫자 PK이므로 Maximo 채번으로 생성하고 이후 유지한다.
- 갱신 조회에는 `원천 시스템 + 원천 개체 종류 + 원천 PK`를 사용한다.
- sourceId는 이 복합키로 고정하고 문자열 ACTCINUM에 저장해 그 고유 키로 MERGE한다.
  예: `D42:DEVICE:100`, `D42:PART:100`, `D42:IPADDRESS:100`.
- 신규 ACTCINUM이면 ACTCIID를 채번하고, 기존 ACTCINUM이면 같은 ACTCIID의 본체·스펙을 갱신한다.
- 원천 종류는 분류명·구현 클래스명·뷰 버전을 사용하지 않는다. 물리/가상 Computer는 DEVICE,
  CPU·RAM·GPU가 같은 Part 개체 공간을 쓰면 PART로 구분한다.
  같은 개체를 다른 뷰에서 표현하는 경우 대표 개체 키를 먼저 정한다.
- 스펙의 REFOBJECTID는 조회·생성한 ACTCIID, 관계의 SOURCECI·TARGETCI는 각 끝점의 ACTCINUM을 사용한다.
- 별도 소스→Target 대응 테이블 없이 ACTCI에서 갱신 대상을 조회한다.

2026-09-14 [식별자 저장 확인](../../knowledge/maximo/ci-model.md#ci-식별자-문자열-저장-확인--2026-09-14)에서
ACTCINUM은 VARGRAPHIC(150)·UPPER(150)이며 숫자 전용 DB 제약이 없음을 확인했다.
사용자는 임의 문자열 ACTCINUM을 넣은 테스트 데이터의 화면 표시와 CI 승격 성공을 보고했다.
정확한 테스트 문자열은 제공되지 않았으며 에이전트가 직접 재현한 시험은 아니다.
숫자 PK의 시퀀스 예약·공존 구현은 별도 확인한다.
원천 UUID를 GUID나 ACTCIID 대신 사용하는 것으로 합의한 것은 아니다.
원천 ID 변경으로 남은 이전 CI의 삭제·보존은 원천 소실 정책에서 별도로 정한다.

## CI 기준 스펙 선택 — 2026-09-14 합의

CI.COMPUTERSYSTEM의 18개 ASSETATTRID를 기본 대조 범위로 사용하고 BIOS·CPU 코어 수를 추가한다.
[대조 결과](../../knowledge/maximo/computer-classification-specs.md)에서
물리·가상 ACTCI 분류에 18개 모두 같은 속성·섹션·자료형으로 대응함을 확인했다.

- CI 분류는 수집 속성을 고르는 기준이다. 본체 분류는 합의한 SYS.COMPUTERSYSTEM / SYS.VIRTUALCOMPUTERSYSTEM을 유지한다.
- ACTCISPEC에는 해당 ACTCI 분류의 CLASSSPECID·CLASSSTRUCTUREID와 ACTCI용 적용 설정을 사용한다. CI 분류의 템플릿 ID를 복사하지 않는다.
- D42 대응·의미·단위를 재조사하고 매핑 문서에 기록했다. 미대응 항목도 명시했으며 18개 모두 값이 수집된다는 뜻은 아니다.
- BIOS와 CPU 코어 수는 1차 포함으로 합의했다. 필요한 추가 정의와 등록 전제는 매핑 문서를 따른다.
- CI.VIRTUALCOMPUTERSYSTEM 자체의 등록 스펙은 14개다. 공통 18개를 ACTCI에 적재할 수 있다는 사실과 승격 후 속성 전달은 구분하며, 승격 대상 분류별 전달은 별도 검증한다.

## 수집 대상과 스펙으로 넣을 정보

| 수집 항목 | 추천하는 관리 단위 | 분류 / 스펙 대응 | 근거·조건 |
| --- | --- | --- | --- |
| Computer | 독립 CI | 물리: SYS.COMPUTERSYSTEM, 가상: SYS.VIRTUALCOMPUTERSYSTEM | 상세 OS별 분류로 시작하지 않고 원천 type으로 구분하는 안. 운영 포함 필터는 별도 확정 |
| 이름·시리얼·UUID·모델·제조사 | Computer 스펙 | COMPUTERSYSTEM_NAME / SERIALNUMBER / UUID / MODEL / MANUFACTURER | 각 이름 앞 COMPUTERSYSTEM_ 접두어. 원천 해당 필드와 Hardware·Vendor 조인 사용 |
| BIOS | Computer 스펙 | 현재 매핑 문서의 추가 수집 표 참조 | ROMVERSION 의미 확인 완료. 출시일은 ALN 추가 정의로 원문 보존 |
| 메모리 총량 | Computer 스펙 | COMPUTERSYSTEM_MEMORYSIZE | device.ram + ram_size_type. 표준 저장 단위 정의 후 변환; 개별 RAM 파트 합계로 대체하지 않음 |
| CPU 수·코어 수·속도 요약 | Computer 스펙 | 현재 매핑 문서의 값 매핑 참조 | D42가 해당 장비에 보고한 구성 기준. VM 값을 호스트 물리 코어 수로 해석하지 않음 |
| 개별 CPU | 독립 CI | SYS.CPU — CPU_CPUCORESINSTALLED / CPUSPEED / MANUFACTURER 등 | 개별 파트 단위 코어·성능·제조사 보존 및 Computer CONTAINS 1:N 설정 존재. 슬롯·모델 필드는 아래 보완 대상 |
| 개별 RAM 모듈 | 이번 기본안에서는 독립 CI 보류; 원천 상세는 유지 | SYS.MEMORY 후보는 있으나 모듈 슬롯·모델·제조사·시리얼 전용 스펙 부족 | Computer→Memory가 1:1이며 모듈은 여러 개일 수 있음. 총량 스펙을 우선 사용; 모듈 관리가 필요하면 적절한 분류·상세 스펙·1:N 관계 보완 |
| 디스크 | 독립 CI | DEV.DISKDRIVE — DISKDRIVE_DISKSIZE, MEDIAACCESSDEVICE_MODEL / MANUFACTURER / SERIALNUMBER | 복수 디스크를 각각 표현하고 Computer CONTAINS 1:N 사용 가능. 용량 단위·인터페이스 코드 변환은 별도 |
| 파일시스템 | 독립 CI | SYS.FILESYSTEM — FILESYSTEM_MOUNTPOINT / TYPE / CAPACITY / AVAILABLESPACE | 복수 마운트 경로·종류·용량을 보존. Computer CONTAINS 1:N 설정 존재. 디스크와 동일 CI로 합치지 않음 |
| OS | 장비별 설치 OS를 독립 CI | SYS.OPERATINGSYSTEM — OPERATINGSYSTEM_NAME / OSVERSION / VERSIONSTRING | 제품 마스터 os_pk가 아닌 deviceos_pk의 설치 행 기준. INSTALLEDON으로 Computer 연결 후보 |
| 네트워크 인터페이스 | 인터페이스별 독립 CI | NET.L2INTERFACE — L2INTERFACE_NAME / HWADDRESS / SPEED / MTU | Computer의 PRIMARYMACADDRESS 한 값으로 모든 포트를 대체하지 않음. 원천 포트 종류와 L2 의미가 맞는 행에 적용; 논리 IP 인터페이스와 일괄 동일시하지 않음 |
| IP | 독립 CI, 관계 보완을 전제로 포함 | NET.IPADDRESS — IPADDRESS_STRINGNOTATION | 공유 IP의 복수 Computer 연결 보존. 현재 Computer/L2Interface와 직접 분류쌍 규칙이 없으므로 정의 보완 또는 IPInterface 경유 모델 검토 |
| 서브넷·게이트웨이 | IP의 보강 정보로 조사 유지; 저장 위치는 별도 결정 | NET.IPNETWORK에 PREFIXLENGTH·NETMASK·SUBNETADDRESS 존재. NET.IPADDRESS에는 해당 전용 스펙 없음 | Subnet CI를 추가할지 IP 스펙을 확장할지 결정. gateway를 IPADDRESS_ADDRESSSPACE 등에 임의 대입하지 않음 |
| 설치 SW | 설치 건별 독립 CI | APP.SOFTWAREINSTALLATION — SOFTWAREINSTALLATION_PRODUCTNAME / VERSIONSTRING / MANUFACTURERNAME / INSTALLEDLOCATION | 이름·버전·제조사·설치 경로에 대응. SOFTWAREIMAGE·SOFTWAREMODULE을 이름 유사성만으로 대체 사용하지 않음 |
| GPU | 이번 기본안에서는 보류 | CARD·SYSTEMBUSCARD 범용 분류는 있으나 GPU 전용 대응·Computer 포함 관계 미확정 | 개별 GPU 관리 범위와 물리/가상 GPU 의미를 확인한 뒤 분류·관계 보완. CPU·메모리 총량 속성에 섞지 않음 |
| Host→VM | 기존 Computer CI 사이 관계 | RELATION.VIRTUALIZES 후보 | 토폴로지 의미 방향. 원천은 VM의 virtual_host_device_fk가 Host를 참조하며 실제 저장 순서·카디널리티 검증 필요 |
| 섀시·VM 관리 장비 | 원천 연결은 유지, 별도 확대 단계 | 분류·관계 미선정 | Computer 분류나 VIRTUALIZES로 일괄 연결하지 않음 |

기본안의 독립 CI는 **Computer·CPU·Disk·Filesystem·OS·Interface**이며,
**IP·설치 SW도 독립 CI로 설계하되 아래 연결 보완을 선행**한다.
Computer의 메모리 총량·CPU 요약과 개별 CPU의 상세 스펙은 집계 수준이 다르다.
요약 값 보유를 이유로 개별 파트 관계를 지우거나, 반대로 모든 요약을 파트 합계로 재계산하지 않는다.
개별 RAM·GPU 보류는 현재 원천 데이터의 건수·누락률에 근거한 제외가 아니다.

## 원천 필드별 스펙 대조·보완

아래는 초기 대응 후보이며 Computer 본체·스펙의 재대조 결과·SQL·값 변환은 현재 매핑 문서를 따른다. 연관 CI는 유형별 확정 시 옮긴다.

| D42 원천 | 넣을 CI와 스펙 후보 | 처리 구분 |
| --- | --- | --- |
| device.name / serial_no / uuid | Computer: COMPUTERSYSTEM_NAME / SERIALNUMBER / UUID | 직접 대응 후보; 본체 ACTCINAME·식별자 역할은 별도 |
| hardware.name / vendor.name | Computer: COMPUTERSYSTEM_MODEL / MANUFACTURER | 직접 대응 후보 |
| device.type / virtualsubtype | Computer 분류 분기 및 VIRTUAL | TYPE은 CDM 유형 ComputerSystem 상수로 매핑. physical/virtual을 TYPE에 넣지 않음 |
| device.last_discovered | Computer 본체 ACTCI.LASTSCANDT | 스펙이 아닌 본체 필드; 누락 처리 정책 필요 |
| BIOS vendor.name | Computer: COMPUTERSYSTEM_BIOSMANUFACTURER | 직접 대응 후보 |
| device.bios_version / bios_release_date | Computer: ROMVERSION / 추가 정의 BIOSRELEASEDATE | 2026-09-15 전역 속성·CI 분류 등록. ACTCI 템플릿은 미등록 |
| device.ram + ram_size_type | Computer: COMPUTERSYSTEM_MEMORYSIZE | 단위 변환 필요 |
| device.total_cpus / core_per_cpu / cpu_speed | Computer: NUMCPUS / CPUCORESINSTALLED / CPUSPEED | 파생 코어 수 의미·속도 단위 확인 |
| CPU partmodel.cores / speed + speed_unit / vendor.name | CPU: CPU_CPUCORESINSTALLED / CPUSPEED / MANUFACTURER | 코어 의미·단위 확인 후 대응 |
| CPU part.slot / partmodel.name / part.serial_no | CPU: INDEXORDER / CPUTYPE 후보; 문자열 시리얼 전용 스펙 미확인 | 순서와 슬롯, CPU 종류와 모델을 구분. IDENTIFYINGNUMBER(NUMERIC)에 시리얼·원천 PK를 대신 넣지 않음 |
| RAM part 슬롯·시리얼·모델·용량·단위 | 기본안은 개별 모듈 저장 보류 | SYS.MEMORY의 BASEADDRESS를 슬롯명으로 사용하지 않음. 상세 관리 요구 시 전용 구조 보완 |
| disk partmodel.name / vendor.name / part.serial_no | Disk: MEDIAACCESSDEVICE_MODEL / MANUFACTURER / SERIALNUMBER | 직접 대응 후보 |
| disk partmodel.hdsize + hdsize_unit | Disk: DISKDRIVE_DISKSIZE | 단위 변환 필요 |
| disk hddtype_name / media_type_name | Disk: DISKINTERFACETYPE / ISSOLIDSTATE 후보 | 문자열→숫자 인터페이스 코드 의미 및 SSD 판정 규칙 확인; 임의 숫자화 금지 |
| mount.mountpoint / fstype_name | Filesystem: FILESYSTEM_MOUNTPOINT / TYPE | 직접 대응 후보 |
| mount.capacity / free_capacity | Filesystem: FILESYSTEM_CAPACITY / AVAILABLESPACE | 원천·Target 저장 단위 확인 |
| mount.filesystem / label | Filesystem의 전용 추가 스펙 후보 | 장치·공유 경로 문자열과 볼륨 라벨을 MOUNTPOINT에 덮어쓰지 않음 |
| deviceos.os_name / os_version | OS: OPERATINGSYSTEM_NAME / OSVERSION 또는 VERSIONSTRING | 이름·버전 문자열의 역할을 확정하고 중복 저장 여부 결정 |
| deviceos.os_version_no / OS vendor.name | OS: BUILDLEVEL 후보 / 제조사 전용 스펙 보완 후보 | 버전 번호와 빌드 의미 확인; 제조사 전용 스펙은 선택 분류에서 미확인 |
| netport.port / hwaddress | Interface: L2INTERFACE_NAME / HWADDRESS | L2 의미에 맞는 포트의 직접 대응 후보 |
| netport.port_speed / mtu | Interface: L2INTERFACE_SPEED / MTU | 속도 형식·단위 확인; 값이 현재 없어도 원천 필드를 비대상으로 확정하지 않음 |
| netport.type_name / global_type / physical_state / vendor.name | 포트 유형 판정 및 추가 스펙 후보 | 상태·유형의 의미를 맞추지 않은 숫자 대입 금지 |
| ip.ip_address | IP: IPADDRESS_STRINGNOTATION | HOST(ip_address)로 주소 표현 후보; 주소 계열과 식별 범위 정의 |
| subnet.mask_bits / gateway | Prefix는 IPNETWORK_PREFIXLENGTH; gateway는 대응 미확정 | Subnet CI 추가 또는 스펙 확장 선택 필요 |
| software.name / vendor.name / softwareinuse.version / install_path | SW Installation: PRODUCTNAME / MANUFACTURERNAME / VERSIONSTRING / INSTALLEDLOCATION | 모두 SOFTWAREINSTALLATION_ 접두어. 직접 대응 후보 |
| softwareinuse.install_date / first_detected / last_updated | 설치·탐지 시각 스펙 보완 후보 | 일반 변경·발견 시각에 무조건 대입하지 않음. 설치 날짜 전용 스펙은 선택 분류에서 미확인 |

## 관계 적용 추천과 남은 확인

- Computer→CPU·Disk·Filesystem·L2Interface는 정확한 분류쌍의 CONTAINS 1:N을 우선 검토한다.
- OS→Computer는 설치 의미와 복수 OS를 표현하는 INSTALLEDON(N:1)을 우선 검토한다. SWAPPED=1·REVRELATIONSHIP=1의 실제 적재·표시는 검증한다.
- SW Installation→OS의 INSTALLEDON(N:1)이 존재한다. 원천 SW는 device_fk로 장비에 연결되므로, 같은 장비에 OS가 하나인 경우의 연결 조건과 OS가 없거나 여럿인 경우를 구분해야 한다. OS에 무조건 곱조인하지 않는다. Computer 직접 설치 관계를 사용할 경우 새 분류쌍 규칙이 필요하다.
- IP는 원천의 device_fks·netport_fk가 명시하는 연결을 보존한다. 기존 NET.IPINTERFACE 경유 BINDSTO 설정이 있다고 해서 존재하지 않는 중간 원천 개체를 자동 생성하지 않는다. 공유 IP에 맞는 카디널리티와 직접 연결 규칙 보완안을 검토한다.
- Host→VM의 VIRTUALIZES를 토폴로지 의미 방향으로 사용한다. 등록 규칙이 1:1·SWAPPED=1이므로 원천 다중 VM 연결과 물리 저장 순서는 검증하기 전 확정하지 않는다.
- 분류의 PARENT 값으로 ACTCIRELATION을 생성하지 않는다. D42 연결 근거와 관계 규칙을 모두 만족하는 쌍을 사용한다.

## 미결 추적

관리 단위·포함 범위는 [ISSUE-8](../../open-issues.md#issue-8-actual-ci-대상-범위),
속성 의미·단위·식별자·관계 적용은 [ISSUE-11](../../open-issues.md#issue-11-actual-ci-분류속성관계와-식별자-매핑)에서 추적한다.
확정된 필드 매핑·변환·SQL은 [Device 매핑](../../data-mapping/ci/types/device.md)에 반영한다.
