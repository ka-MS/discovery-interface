# OS CI 수집 설계

> 상태: 추천안 · 작성 2026-09-15. 분류·스펙·관계는 조사 근거에 기반한 추천이며 사용자 확정 전이다.
> 원천 관측: [OS·Disk·Filesystem·IP 원천 조사](../../knowledge/device42/ci-component-inventory.md)
> 타겟 관측: [OS·Disk·Filesystem·IP 분류 조사](../../knowledge/maximo/ci-component-classifications.md)
> 확정 매핑: [OS 매핑](../../data-mapping/ci/types/os.md)

수치는 `.68 / .35` 순이다.

## 1. 관리 단위 — 독립 CI

2026-09-15 사용자 합의다. OS를 Computer 스펙으로 흡수하지 않고 별도 ACTCI로 만든다.

OS는 관측상 장비당 정확히 1개다. 두 서버 모두 `MAX(장비당 OS 수)=1`이고 2개 이상인 장비가 없다.
다중성만 보면 Computer 스펙으로도 담을 수 있다. 그럼에도 독립 CI로 두는 이유는 두 가지다.

- 원천이 `eol`·`eos`를 48/22, 47/22건 갖는다. 날짜형이라 빈 문자열 문제가 없다. 사업 범위 「나. H/W, S/W, 네트워크, 라이선스 등 IT자산별 EOS 관리」의 대상이며, EOS는 Computer가 아니라 OS에 붙는 속성이다.
- OS 제품·제조사·버전이 `view_os_v1` → `view_vendor_v1`로 별도 마스터를 이룬다. Computer 스펙으로 평탄화하면 이 참조가 사라진다.

## 2. 분류 선택 — SYS.OPERATINGSYSTEM

| 후보 | CLASSSTRUCTUREID | 스펙 | 판정 |
| --- | --- | ---: | --- |
| **SYS.OPERATINGSYSTEM** | CCI10538 | 50 | **추천** |
| SYS.WINDOWS.WINDOWSOPERATINGSYSTEM | CCI10519 | 50 | 부적합 |
| SYS.I5OS.I5OPERATINGSYSTEM | CCI10500 | 79 | 부적합 |

원천 OS가 한 계열이 아니다. 관측된 제품군은 Red Hat·Ubuntu·CentOS·Debian·Alpine·VMware Photon·
ESXi·Windows(10/11/Server)·macOS·FreeBSD·Solaris·Cisco IOS·VxWorks·Samsung firmware다.
계열별 분류를 쓰면 제품명으로 분류를 라우팅해야 하고, Alpine·Photon처럼 대응 분류가 없는 계열은
적재할 수 없다. Computer가 범용 `SYS.COMPUTERSYSTEM`을 쓴 선례와도 맞는다.

`SYS.OPERATINGSYSTEM`은 ACTCI 적용 스펙 50개를 갖고, 전부 전역 정의이며 필수 속성이 없다.

## 3. 스펙 대조표

**대조 기준은 `CI.OS`(CCI00013) 7개다.** 적재 대상은 ACTCI 쪽 `SYS.OPERATINGSYSTEM`이지만,
수집 속성 선택은 CI 계열을 기준선으로 삼는다. Computer가 `CI.COMPUTERSYSTEM` 18개를
기준으로 삼은 것과 같다. 관측은 [분류 조사](../../knowledge/maximo/ci-component-classifications.md) 6절.

| ASSETATTRID | CI.OS | 자료형 | 원천 | 채택 | 비고 |
| --- | :---: | --- | --- | --- | --- |
| OPERATINGSYSTEM_OSNAME | ○ | ALN | `o.os_name` | 채택 | 전건 보유 |
| OPERATINGSYSTEM_NAME | ○ | ALN | `o.os_name` | 채택 | CI 기준 속성. OSNAME과 같은 값 |
| OPERATINGSYSTEM_OSVERSION | ○ | ALN | `o.os_version` | 채택 | 41 / 37건. 공백 제외 |
| OPERATINGSYSTEM_KERNELVERSION | ○ | ALN | `o.os_version_no` | 채택 | 23 / 19건. 공백 제외 |
| OPERATINGSYSTEM_FQDN | ○ | ALN | – | 미채택 | 원천 없음 |
| OPERATINGSYSTEM_OSMODE | ○ | ALN | – | 미채택 | 원천 없음 |
| OPERATINGSYSTEM_OSCONFIDENCE | ○ | NUMERIC | – | 미채택 | 원천 없음 |
| **OPERATINGSYSTEM_KERNELARCHITECTURE** | **✗** | ALN | `o.os_arch_name` | **채택** | **CI 기준 밖 의도적 추가.** 아래 참조 |
| 나머지 OPERATINGSYSTEM_ 29개 | ✗ | – | – | 미채택 | `SYS.OPERATINGSYSTEM`에만 있고 원천 대응 없음 |

`MODELOBJECT_CDMSOURCE`·`SOURCETOKEN`은 두 분류 모두에 있으나 채택 여부 미정이다.

### CI 기준 밖 추가 — KERNELARCHITECTURE

`OPERATINGSYSTEM_KERNELARCHITECTURE`는 `CI.OS`에 없고 `SYS.OPERATINGSYSTEM`에만 있다.
원천 `os_arch_name`이 21 / 19건 있어 채택했다.

Computer가 `CI.COMPUTERSYSTEM` 18개에 BIOS 출시일·CPU 코어 수를 더한 것과 같은 성격이다.
기준선을 벗어나는 추가는 이유와 함께 명시한다는 관례를 따른다.

**승격 시 누락 위험이 있다.** `SYS.OPERATINGSYSTEM`은 승격 범위에서 `CI.OS`로 매핑되는데
(`CITEMPLATE` 71번), `CI.OS`에 이 속성이 없으므로 승격에서 값이 전달되지 않을 수 있다.
실제 동작은 확인한 적이 없다. 이 ETL은 승격을 구현하지 않는다.
설정은 [CI 승격 범위](../../knowledge/maximo/ci-promotion-scope.md) 참조. ISSUE-11.

### 미대응 — EOL·EOS

`eol`·`eos`에 대응하는 속성이 `SYS.OPERATINGSYSTEM`에 없다. `LIFECYCLESTATE`는 NUMERIC 코드이고
날짜 속성이 아니다. 사업 범위 「나」에 직결되므로 아래 중 하나를 정해야 한다.

1. 전역 ALN 속성 `OPERATINGSYSTEM_EOL`·`OPERATINGSYSTEM_EOS`를 신규 등록한다. Computer BIOS 출시일과 같은 방식이며 기준정보 변경 승인이 필요하다.
2. 이번 범위에서 제외하고 EOS 관리 기능 설계 시 함께 정한다.

추천은 2다. EOS는 자산 EOS 관리 프로세스 전체와 묶이는 사안이라 CI 속성 하나로 끝나지 않는다.
이번에는 원천 보유 사실만 기록한다. ISSUE-11에서 추적한다.

## 4. 본체 필드

| ACTCI 컬럼 | 원천 | 비고 |
| --- | --- | --- |
| ACTCINUM | `D42:DEVICEOS:<deviceos_pk>` | 5절 |
| ACTCINAME | `o.os_name` | 전건 보유 |
| CLASSSTRUCTUREID | SYS.OPERATINGSYSTEM 조회값 | 상수 고정 안 함 |
| DESCRIPTION | 대응 없음 | 원천에 메모 필드가 없다 |
| LASTSCANDT | **미결** | 6절 |
| CHANGEBY / LANGCODE / CHANGEDATE / HASLD | Computer와 동일 규약 | `Device42` / `KO` / 매핑 시각 / 0 |

### LASTSCANDT 미결

`view_deviceos_v1`에 `last_discovered`가 없다. 전건 보유 컬럼은 `first_added`·`last_edited`이며
둘 다 레코드 변경 시각이지 발견 시각이 아니다. 선택지는 셋이다.

1. `last_edited`를 쓴다. 의미가 어긋나지만 값이 전건 있다.
2. 부모 Computer의 `view_device_v2.last_discovered`를 쓴다. OS는 그 장비 스캔으로 발견된 것이므로 의미가 가깝다.
3. NULL을 전달한다. ACTCI.LASTSCANDT가 NOT NULL이면 본체 저장이 실패한다.

추천은 2다. Computer가 이미 같은 값을 쓰고 있고, OS 발견 시점이 곧 그 장비의 스캔 시점이다.
Computer를 조인해야 하므로 조회 SQL에 반영한다.

## 5. 식별자

`ACTCINUM = D42:DEVICEOS:<deviceos_pk>`.

`deviceos_pk`는 두 서버 모두 전건 유일하다. Computer의 `D42:DEVICE:<pk>` 규약과 같은 형식이며,
원천 시스템·개체 종류·원천 PK 세 요소를 담는다. 근거는 [Computer 수집 설계](computer.md)의
CI 갱신 추적 키 절이다.

## 6. 관계 추천안

**확정이 아니다.** 규칙 존재는 관측 사실이고, 어느 코드를 쓸지는 결정 대상이다.

`SYS.OPERATINGSYSTEM` → Computer 방향으로 두 규칙이 있다. 양쪽 Computer 분류 모두 동일하다.

| RELATIONNUM | CONTAINMENT | CARDINALITY | 의미 |
| --- | ---: | --- | --- |
| RELATION.INSTALLEDON | 1 | N:1 | 설치됨. 포함 관계 |
| RELATION.RUNSON | 0 | 1:1 | 실행됨. 비포함 |

추천은 **RELATION.INSTALLEDON**이다.

- `N:1`이라 장래 한 장비에 OS가 둘 이상(멀티부트, 컨테이너 호스트) 생겨도 규칙을 안 바꾼다. `RUNSON`의 `1:1`은 그때 깨진다.
- `CONTAINMENT=1`이라 Computer 하위 계층으로 표현된다. Disk·Filesystem이 `CONTAINS`로 붙는 것과 계층이 일치한다.
- 원천 `view_deviceos_v1`은 "장비에 설치된 OS"이므로 의미가 맞는다.

출발이 OS, 도착이 Computer다. Disk·Filesystem과 방향이 반대이므로 적재 시 헷갈리지 않게 한다.

**주의:** `RELATION.INSTALLEDON`의 `USEWITH`는 `CI`이고 `ACTCI`가 아니다.
`RELATIONRULES`의 분류쌍 조건은 만족하지만 관계 정의가 ACTCI용으로 표시돼 있지 않다.
실제 적재·UI 표시는 미검증이다. ISSUE-11에서 다룬다.

## 7. 수집 대상 범위

| 범위 | 건수 | 비율 |
| --- | ---: | ---: |
| 전체 OS | 88 / 79 | 100% |
| Computer 연결 OS | 26 / 63 | 30% / 80% |

추천은 **Computer 연결분만**이다.

- 사업 범위의 수집 구분은 「서버」다. Computer 외 장비의 OS는 네트워크 장비(Cisco IOS)나 미분류 장비의 것이다.
- Computer에 연결되지 않은 OS는 6절의 관계를 만들 대상이 없어 고아 CI가 된다.
- 네트워크 장비를 CI로 확장할 때 그 유형의 OS를 함께 넣는 편이 관계 설계가 단순하다.

두 서버 비율 차이가 크다(30% 대 80%). 운영 D42 한 대 기준으로 다시 확인해야 한다.

## 8. 미결

| 항목 | 상태 | 추적 |
| --- | --- | --- |
| EOL·EOS 속성 미등록 | 이번 범위 제외 추천 | ISSUE-11 |
| LASTSCANDT 원천 | 부모 Computer 값 사용 추천 | ISSUE-11 |
| 관계 코드 선택과 ACTCI 적용 여부 | INSTALLEDON 추천, USEWITH=CI 미검증 | ISSUE-11 |
| 수집 대상 범위 | Computer 연결분 추천 | ISSUE-8 |
| OPERATINGSYSTEM_VERSIONSTRING | CI 기준 밖. OSVERSION과 중복이라 미채택 | – |
| KERNELARCHITECTURE 승격 전달 | CI 기준 밖 속성의 승격 동작 미검증 | ISSUE-11 |
