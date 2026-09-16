# Device 통합 수집의 Maximo 분류·스펙

> 관측: 2026-09-15, 재확인 2026-09-16 · MAXIMO / BLUDB, 읽기 전용.
> 재조회: [분류 탐색](../../exploration-queries/maximo/device-ci-classification-discovery.sql), [스펙·관계](../../exploration-queries/maximo/device-ci-specs.sql), [속성 후보·승격](../../exploration-queries/maximo/device-ci-target-gaps.sql).
> 로컬 원본: `local/db-access-kit/work/device-ci-unification-20260915/maximo/`.
> 2026-09-16 재확인 원본: `local/db-access-kit/work/device-ci-phase2-20260916*/`.

## 1. 분류와 적용 상태

| CLASSIFICATIONID | 적용 객체 | 스펙 수 | 속성 ID 연결·ACTCI USEINSPEC=1 | 관측 의미 |
| --- | --- | ---: | ---: | --- |
| SYS.COMPUTERSYSTEM | ACTCI | 98 | 98 | 기존 물리 Computer |
| SYS.VIRTUALCOMPUTERSYSTEM | ACTCI | 98 | 98 | 기존 VM |
| SYS.GENERICSWITCH | ACTCI | 99 | 99 | 장비 수준 스펙을 가진 Switch 후보 |
| SYS.GENERICROUTER | ACTCI | 99 | 99 | 장비 수준 스펙을 가진 Router 후보 |
| SYS.APPLIANCE.NETWORKSYSTEM | ACTCI | 116 | **0** | CLASSSPEC은 있으나 ASSETATTRIBUTE 연결·적용 행 미보완 |
| SYS.PRINTER | ACTCI | 18 | 18 | FUNCTION 4개 + MODELOBJECT 14개 |
| NET.NETWORKING | ACTCI | 18 | 18 | FUNCTION 계열 |
| NET.BRIDGE | ACTCI | 19 | 19 | FUNCTION 계열 + BRIDGE_BRIDGEID |
| CI.PRINTER | CI | 1 | 해당 없음 | FUNCTION_NAME 한 개 |
| CI.COMPUTERSYSTEM | CI | 19 | 해당 없음 | 기존 Computer 스펙 선정 기준 |

GenericSwitch·GenericRouter는 Computer의 98개 ASSETATTRID를 모두 가지며
`GENERICCOMPUTERSYSTEM_GENERICTYPE`(ALN) 하나가 추가된다.
세 분류의 해당 ACTCI 적용 행에서 속성명·섹션 일치, 전역 조직/사이트 범위를 확인했다.
분류 ID 숫자·스펙 ID는 환경 조회값이며 코드에 하드코딩하지 않는다.

2026-09-16 재확인 결과 일반 Ethernet Switch 본체에 맞는 기존 Authorized CI 분류는 없다.
`CI.FCSWITCHFUNCTION`과 `CI.IPSTORAGESWITCHFUNCTION`은 각각
FUNCTION_MANAGEDSYSTEMNAME·FUNCTION_NAME 두 속성뿐이다. `CI.GENERIC_COMPUTERSYSTEM`도
COMPUTERSYSTEM_SERIALNUMBER 한 속성뿐이고 정상적인 CI 계층 부모가 없다.
물리 Switch 본체 분류로 재사용하기에는 부족하다.
추천 Printer 전용 속성명 `PHYSICALPRINTER_TRAYCOUNT`, `PHYSICALPRINTER_FIRMWARENAME`,
`PHYSICALPRINTER_FIRMWAREVERSION`은 2026-09-16 전역 ASSETATTRIBUTE에 존재하지 않았다.

현재 제품 `ComputerSpec` 18개 중 `COMPUTERSYSTEM_BIOSRELEASEDATE`는 위 98/99개 템플릿에 없다.
기존 코드는 전역 추가 속성 경로를 사용한다. 통합 시 이 경로와 VM_ID 적용 조건을 보존해야 한다.

## 2. SYS.PRINTER는 물리 프린터 본체 분류와 동일시할 수 없음

관측된 SYS.PRINTER의 스펙은 FUNCTION_NAME·MANAGEDSYSTEMNAME·LOCATIONTAG·ISPLACEHOLDER와
MODELOBJECT 공통 14개뿐이다. 모델·제조사·시리얼·RAM·트레이 전용 스펙은 없다.
분류 PARENT가 SYS.COMPUTERSYSTEM이어도 Computer 98개 속성이 실제 템플릿에 포함되어 있지 않다.

현재 규칙은 `SYS.COMPUTERSYSTEM → SYS.PRINTER`, `RELATION.PROVIDES`, 1:N, CONTAINMENT=1이다.
SYS.PRINTER → NET.IPINTERFACE 포함 규칙은 이번 조회에서 없었다.
IBM [Integration Composer 설명](https://www.ibm.com/docs/SSSQ39/ICDwiki.pdf)도 Router·Printer를
Function 인스턴스로 설명하며 물리 ComputerSystem의 모델·제조사와 구분한다.
이는 기존 템플릿·규칙과 함께 기능 개체로 해석할 근거이며, 새 적재 방식의 검증을 대신하지 않는다.

따라서 Device 한 행을 SYS.PRINTER로 분류하는 것만으로 물리 프린터의 장비 정보를 충분히
저장할 수 있다는 결론은 성립하지 않는다. 본체 분류 선택은 [ISSUE-11](../../open-issues.md#issue-11-actual-ci-분류속성관계와-식별자-매핑).

## 3. 원천과 대응할 속성

Computer·GenericSwitch·GenericRouter에 공통으로 존재한다.

| ASSETATTRID | 자료형 | 원천 대조에 쓸 의미 |
| --- | --- | --- |
| COMPUTERSYSTEM_NAME / MODEL / MANUFACTURER / SERIALNUMBER / UUID | ALN | 이름·모델·제조사·시리얼·UUID |
| COMPUTERSYSTEM_MEMORYSIZE / NUMCPUS / CPUSPEED / CPUCORESINSTALLED | NUMERIC | 자원 요약. 원천 단위와 의미 대조 필요 |
| COMPUTERSYSTEM_CPUTYPE / ARCHITECTURE | ALN | CPU 모델·아키텍처 |
| COMPUTERSYSTEM_PRIMARYMACADDRESS | ALN | 대표 MAC 선정 근거 필요 |
| COMPUTERSYSTEM_BIOSMANUFACTURER / ROMVERSION | ALN | BIOS 제조사·버전 |
| COMPUTERSYSTEM_ASSETTAG / LOCATIONTAG | ALN | 자산 태그·위치 태그 후보. 원천 asset_no·위치 필드와의 동일 의미는 별도 결정 |
| COMPUTERSYSTEM_TYPE / VIRTUAL / VMID | ALN | 유형·가상 여부·VM 식별. 분류별 적용값 결정 필요 |
| COMPUTERSYSTEM_BIOSDATE | NUMERIC | 원문 날짜 문자열을 그대로 넣을 수 없음 |

세 분류에 OS 이름·OS 버전·하드웨어 EOL/EOS·스레드 수·프린터 트레이 전용 속성은 확인되지 않았다.
범용 MODELOBJECT 속성이나 lifecycle 숫자에 다른 의미의 값을 넣지 않는다.
SYS.APPLIANCE.NETWORKSYSTEM에는 `SCOMPUTERSYSTEM_OSNAME/OSVERSION` 등이 있지만
필수 연결·적용 설정이 비어 있으므로 즉시 사용 가능한 대안은 아니다.

`SNMPSYSTEMGROUP_SYSNAME`은 별도 `SYS.SNMPSYSTEMGROUP`의 ALN 속성으로 확인했다.
그 분류에는 SYSDESCR·SYSLOCATION·SYSCONTACT·SYSOBJECTID도 있다.
Device 이름을 sysName으로 간주하거나 해당 템플릿을 GenericSwitch의 속성으로 복사하지 않는다.

## 4. 관계·승격 범위

- GenericSwitch/Router → NET.IPINTERFACE 및 NET.L2INTERFACE의 CONTAINS 1:N 규칙은 있다.
- GenericSwitch/Router → SYS.SNMPSYSTEMGROUP의 GIVESDETAILS 1:1 규칙은 있다.
- GenericSwitch/Router → SYS.PRINTER의 PROVIDES 1:N 규칙도 있다. 실제 원천 연결 없이는 생성하지 않는다.
- **GenericSwitch·GenericRouter·SYS.PRINTER·SYS.APPLIANCE.NETWORKSYSTEM의 CITEMPLATE 행은 0건이다.**
  ACTCI 속성 적용 가능과 CI 승격 가능은 별개다. 신규 분류 선택 후 승격 대상·범위를 구성하고 검증해야 한다.
- 신규 후보 분류의 ACTCI 실적재는 이번 조사에서 수행하지 않았다. 조회 당시 기존 ACTCI에도 이 네 분류는 없었다.

분류 선택·기준정보 보완은 [Device 통합 설계](../../design/ci/device.md), 상태는 ISSUE-8·11에서 관리한다.
