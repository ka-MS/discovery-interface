# Device CI 기준정보 설계

> 상태: 2차 추천안 완료 · MAS UI 미적용 · 2026-09-16.
> 범위: Switch CI 승격, 물리 Printer ACTCI·CI 분류와 속성 세트.
> 근거: [Device 통합 설계](device.md), [Maximo 분류 관측](../../knowledge/maximo/device-ci-classifications.md), [Device 매핑](../../data-mapping/ci/types/device.md).

이 문서는 MAS의 **분류**와 **승격 범위** 화면에 입력할 설계다. 등록 SQL은 만들지 않는다.
분류 ID는 아래 값을 추천 기본값으로 사용하고, 운영 명명 규칙이 다르면 UI 등록 전에만 바꾼다.
등록 후에는 코드·매핑 문서와 함께 바꿔야 하므로 임의 변경하지 않는다.

## 1. 결정

- Switch Actual CI는 기존 `SYS.GENERICSWITCH`를 그대로 사용한다.
- Switch Authorized CI는 기능 분류를 재사용하지 않고 `CI.GENERICSWITCH`를 새로 만든다.
- 물리 Printer는 기존 기능 분류 `SYS.PRINTER`·`CI.PRINTER`를 사용하지 않는다.
- 물리 Printer 본체용 `SYS.PHYSICALPRINTER`와 `CI.PHYSICALPRINTER`를 새로 만든다.
- 두 승격 범위는 우선 본체 한 종류만 포함한다. Interface·IP·SNMP·기능 Printer는 관계 구현 후 별도로 확장한다.
- 모든 속성은 선택 항목으로 둔다. 원천 누락 때문에 본체 승격 전체가 실패하게 하지 않는다.

기존 `CI.FCSWITCHFUNCTION`과 `CI.IPSTORAGESWITCHFUNCTION`은 각각 Fibre Channel·IP Storage
기능용이며 공통 장비 속성이 없다. `CI.GENERIC_COMPUTERSYSTEM`도 시리얼 한 속성뿐이고
정상적인 CI 계층 부모가 없어 Switch 본체 분류로 사용하지 않는다.

## 2. 분류 구성

| 용도 | CLASSIFICATIONID | 상위 분류 | Use With | Top Level | 처리 |
| --- | --- | --- | --- | --- | --- |
| Switch Actual CI | `SYS.GENERICSWITCH` | 기존 `ACTUALCIROOTCLASS` | ACTCI | 선택 | 기존 분류 수정 |
| Switch Authorized CI | `CI.GENERICSWITCH` | `CI.COMPUTERSYSTEM` | CI | 선택 | 신규 |
| 물리 Printer Actual CI | `SYS.PHYSICALPRINTER` | `ACTUALCIROOTCLASS` | ACTCI | 선택 | 신규 |
| 물리 Printer Authorized CI | `CI.PHYSICALPRINTER` | `CI.COMPUTERSYSTEM` | CI | 선택 | 신규 |

설명은 각각 `Generic network switch`, `Physical network printer`처럼 물리 본체임을 드러내게 쓴다.
조직·사이트는 비워 전역 분류로 두고 `Auto Create CI`는 선택하지 않는다.
상위 ComputerSystem 분류의 속성은 현재 `APPLYDOWNHIER=0`이므로 자동 상속을 기대하지 않고
아래 속성을 각 신규 분류에 직접 등록한다.

`Top Level`은 독립 본체를 승격 시작점으로 선택하기 위해 필요하다. [IBM의 승격 범위 설명](https://www.ibm.com/docs/en/max-it/cd?topic=cis-promotion-scopes)도
시작 Actual CI 분류와 최상위 기능 CI 분류를 지정하고, Authorized CI의 최상위 여부는
분류의 Use With 설정에서 지정하도록 설명한다.

## 3. Switch 속성 세트

### 3.1 Actual CI

`SYS.GENERICSWITCH`의 기존 99개 정의는 삭제하거나 복제하지 않는다. 현재 수집기가 사용하는
아래 속성 중 17개는 이미 연결돼 있다. `COMPUTERSYSTEM_BIOSRELEASEDATE`만 ACTCI 분류에 추가한다.

### 3.2 Authorized CI

`CI.GENERICSWITCH`에는 아래 18개만 등록한다. 승격은 Actual CI와 Authorized CI 양쪽에 같은
ASSETATTRID가 있는 값만 전달하므로, 수집하지 않는 99개 전체를 복사하지 않는다.

| 순서 | ASSETATTRID | 타입 | 단위 | 값/조건 |
| ---: | --- | --- | --- | --- |
| 10 | `COMPUTERSYSTEM_NAME` | ALN | — | `d.name` |
| 20 | `GENERICCOMPUTERSYSTEM_GENERICTYPE` | ALN | — | 단일 판정값 `Switch` |
| 30 | `COMPUTERSYSTEM_MODEL` | ALN | — | Hardware 모델 |
| 40 | `COMPUTERSYSTEM_MANUFACTURER` | ALN | — | Hardware Vendor |
| 50 | `COMPUTERSYSTEM_SERIALNUMBER` | ALN | — | 장비 시리얼 |
| 60 | `COMPUTERSYSTEM_UUID` | ALN | — | 장비 UUID, 빈값 생략 |
| 70 | `COMPUTERSYSTEM_PRIMARYMACADDRESS` | ALN | — | cluster 연결 포트의 대표 MAC |
| 80 | `COMPUTERSYSTEM_TYPE` | ALN | — | `ComputerSystem` |
| 90 | `COMPUTERSYSTEM_VIRTUAL` | ALN | — | `false` |
| 100 | `COMPUTERSYSTEM_MEMORYSIZE` | NUMERIC | GBYTE/MBYTE | 값이 있을 때만 원천 단위 대응 |
| 110 | `COMPUTERSYSTEM_NUMCPUS` | NUMERIC | — | 값이 있을 때만 |
| 120 | `COMPUTERSYSTEM_CPUCORESINSTALLED` | NUMERIC | — | CPU 수 × CPU당 코어, 둘 다 있을 때만 |
| 130 | `COMPUTERSYSTEM_CPUSPEED` | NUMERIC | GHZ/MHZ | 값과 지원 단위가 모두 있을 때만 |
| 140 | `COMPUTERSYSTEM_CPUTYPE` | ALN | — | CPU 모델이 정확히 한 종류일 때만 |
| 150 | `COMPUTERSYSTEM_ARCHITECTURE` | ALN | — | CPU 아키텍처가 정확히 한 종류일 때만 |
| 160 | `COMPUTERSYSTEM_BIOSMANUFACTURER` | ALN | — | 값이 있을 때만 |
| 170 | `COMPUTERSYSTEM_ROMVERSION` | ALN | — | BIOS 버전, 값이 있을 때만 |
| 180 | `COMPUTERSYSTEM_BIOSRELEASEDATE` | ALN | — | 원천 날짜 문자열 보존 |

전역 ASSETATTRIBUTE는 18개 모두 현재 존재한다. `BIOSRELEASEDATE`도 2026-09-15 등록된 ALN 정의를
재사용한다. 모든 속성은 `Use in Specification=선택`, `Mandatory=해제`, 섹션·도메인·기본값 없음으로 둔다.

## 4. 물리 Printer 속성 세트

양 D42 표본은 서버별 한 대이며 모델 `X3220NR`, 제조사 Samsung, 시리얼, RAM `2.048 GB`,
비어 있지 않은 MAC 한 개, 급지 트레이 3개를 가진다. Firmware 이름·버전은
`d.os_name`·`d.os_version`에 있고 BIOS 필드가 아니므로 Computer BIOS 속성에 넣지 않는다.

아래 13개를 `SYS.PHYSICALPRINTER`와 `CI.PHYSICALPRINTER` 양쪽에 같은 순서로 등록한다.
앞의 10개는 기존 전역 ASSETATTRIBUTE를 재사용하고 마지막 3개만 신규 정의한다.

| 순서 | ASSETATTRID | 타입 | 단위 | Source/값 | 정의 |
| ---: | --- | --- | --- | --- | --- |
| 10 | `COMPUTERSYSTEM_NAME` | ALN | — | `d.name` | 기존 |
| 20 | `COMPUTERSYSTEM_MODEL` | ALN | — | `h.name` | 기존 |
| 30 | `COMPUTERSYSTEM_MANUFACTURER` | ALN | — | `v.name` | 기존 |
| 40 | `COMPUTERSYSTEM_SERIALNUMBER` | ALN | — | `d.serial_no` | 기존 |
| 50 | `COMPUTERSYSTEM_UUID` | ALN | — | `d.uuid`, 빈값 생략 | 기존 |
| 60 | `COMPUTERSYSTEM_ASSETTAG` | ALN | — | `d.asset_no`, 빈값 생략 | 기존 |
| 70 | `COMPUTERSYSTEM_PRIMARYMACADDRESS` | ALN | — | 비어 있지 않은 직접 포트 MAC이 정확히 하나일 때 | 기존 |
| 80 | `COMPUTERSYSTEM_MEMORYSIZE` | NUMERIC | GBYTE/MBYTE | `d.ram`, 원천 단위 대응 | 기존 |
| 90 | `COMPUTERSYSTEM_TYPE` | ALN | — | `Printer` | 기존 |
| 100 | `COMPUTERSYSTEM_VIRTUAL` | ALN | — | `false` | 기존 |
| 110 | `PHYSICALPRINTER_TRAYCOUNT` | NUMERIC | — | `printer_input` 파트 행 수 | **신규** |
| 120 | `PHYSICALPRINTER_FIRMWARENAME` | ALN | — | `d.os_name` | **신규** |
| 130 | `PHYSICALPRINTER_FIRMWAREVERSION` | ALN | — | `d.os_version` 원문 | **신규** |

신규 속성 설명은 `Physical printer input tray count`, `Physical printer firmware name`,
`Physical printer firmware version`으로 제안한다. 세 속성 모두 전역, 조직·사이트·도메인·기본 단위 없음으로 둔다.
모든 분류 적용은 `Use in Specification=선택`, `Mandatory=해제`, 섹션·기본값 없음이다.

### 제외하는 속성

- CPU 수·속도·코어·아키텍처: 현재 Printer 표본의 CPU 모델은 일반 문자열이고 속도·코어 근거가 부족하다.
- DPI·최대 용지 크기: D42 대응 원천이 없다. 빈 속성을 미리 만들지 않는다.
- SysName: `d.name`이 SNMP sysName이라는 검증이 없다. `SNMPSYSTEMGROUP_SYSNAME`으로 복사하지 않는다.
- IP Address·Network Interface: 본체 단일 속성으로 압축하지 않고 별도 CI와 관계로 관리한다.
- 토너·드럼·롤러 잔량: 시점성 소모품 정보이며 물리 본체 기준정보가 아니다.
- 논리적 `SYS.PRINTER` 기능 CI: 업무 요구가 생길 때 별도 키와 `PROVIDES` 관계를 설계한다.

## 5. 승격 범위와 관계

| 승격 범위 | 최상위 ACTCI | 최상위 CI | 매핑 |
| --- | --- | --- | --- |
| Generic Switch | `SYS.GENERICSWITCH` | `CI.GENERICSWITCH` | 본체 1:1 한 행 |
| Physical Printer | `SYS.PHYSICALPRINTER` | `CI.PHYSICALPRINTER` | 본체 1:1 한 행 |

이번 승격 범위에는 관련 분류를 추가하지 않는다. 본체만 있는 범위는 새 관계 규칙이 필요하지 않다.
Switch에 이미 있는 `NET.IPINTERFACE`·`NET.L2INTERFACE` 포함 규칙과
`SYS.SNMPSYSTEMGROUP` 상세 규칙은 Actual CI 수집·관계가 구현되지 않았으므로 사용하지 않는다.
Printer도 Interface→IP 경로가 설계되기 전에는 직접 IP 관계를 만들지 않는다.

후속으로 Interface·SNMP를 범위에 넣을 때는 Actual CI 양 끝과 관계를 먼저 적재하고,
대응 CI 분류와 CI 관계 규칙을 함께 구성한 뒤 승격 범위 유효성 검사를 다시 수행한다.

## 6. MAS UI 등록 순서

1. 분류 애플리케이션에서 기존 `SYS.GENERICSWITCH`를 확인하고 신규 CLASSIFICATIONID 세 개의 중복 여부를 확인한다.
2. `CI.GENERICSWITCH`를 만들고 CI Use With·Top Level·상위 `CI.COMPUTERSYSTEM`을 설정한다.
3. 기존 `SYS.GENERICSWITCH`의 ACTCI Use With에서 Top Level을 선택하고 BIOS 출시일 속성을 추가한다.
4. `SYS.PHYSICALPRINTER`와 `CI.PHYSICALPRINTER`를 만들고 2절의 Use With·상위·Top Level을 설정한다.
5. 3·4절 속성을 순서대로 등록한다. 기존 속성은 선택해서 재사용하고 Printer 전용 세 개만 새로 만든다.
6. 승격 범위 애플리케이션에서 5절의 본체 1:1 범위 두 개를 만든다.
7. 각 승격 범위에서 **유효성 검증**을 실행한다.
8. 테스트 Actual CI 한 건씩으로 승격·속성 표시를 확인한 뒤 운영 범위를 확장한다.

승격 범위의 최상위 두 값은 저장 후 직접 변경할 수 없으므로 잘못 등록하면 범위를 삭제하고 다시 만든다.
운영 반영에는 가능하면 MAS Migration Manager 또는 환경의 표준 구성 이관 절차를 사용한다.

## 7. 완료·롤백 기준

### UI 설정 완료 기준

- 네 분류의 Use With·상위·Top Level·전역 범위가 2절과 같다.
- Switch CI 속성 18개, Printer ACTCI·CI 속성 각 13개가 타입·순서·필수 여부와 일치한다.
- Printer 신규 속성은 전역 정의가 각각 한 건뿐이다.
- 두 승격 범위의 유효성 검사가 성공한다.
- Switch 승격 후 CI 분류가 `CI.GENERICSWITCH`이고, 이름·모델·제조사·시리얼·MAC·GenericType이 유지된다.
- Printer 구현 후 승격한 CI에서 이름·모델·제조사·시리얼·RAM·MAC·트레이·Firmware가 유지된다.
- 기존 Computer·VM 승격 범위와 속성에는 변경이 없다.

### 롤백 기준

- 실제 CI를 만들기 전에는 신규 승격 범위부터 삭제하고 신규 분류·Printer 전용 속성을 역순으로 제거한다.
- 기존 `SYS.GENERICSWITCH`는 삭제하지 않는다. 이번에 추가한 BIOS 출시일 스펙과 Top Level 설정만 원복한다.
- Actual CI나 CI가 신규 분류를 참조한 뒤에는 분류를 직접 삭제하지 않는다. 테스트 데이터를 UI에서 정리하거나
  재분류한 뒤 참조가 0건임을 확인하고 설정을 제거한다.
- 직접 DB DELETE/UPDATE로 롤백하지 않는다.

현재 상태는 **설계 완료·MAS UI 미적용**이다. 실제 등록 결과와 화면 캡처·검증 건수는 적용 작업에서 별도로 기록한다.
