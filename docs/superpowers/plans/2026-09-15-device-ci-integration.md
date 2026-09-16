# Device CI 통합 구현 계획

> 상태: 1차 구현·검증, 2차 기준정보 추천 설계 완료 · 3차 미착수 · 2026-09-16.
> 설계 정본: [Device 기준 CI 통합 수집 설계](../../data-analysis/design/ci/device.md).
> 현재 구현 정본: [Device CI 매핑](../../data-analysis/data-mapping/ci/types/device.md).

## 1. 확정한 방향

- Computer·VM·Switch·물리 Printer 본체는 하나의 `DeviceCiIntegrate`에서 처리한다.
- 장비 식별자는 전 유형에서 `D42:DEVICE:<device_pk>`를 유지한다.
- 기존 `CiSourceFilter.COMPUTER`는 OS·Disk·Filesystem·관계가 사용하므로 확대하지 않는다.
- Switch는 물리 장비에서 포트의 `second_device_fk`로 cluster를 찾고,
  cluster의 `details->>'fw_device_type'`이 단일 값 `Switch`일 때만 판정한다.
- 현재 두 D42에서 판별 가능한 네트워크 장비는 Switch 2대뿐이다. Router 표본은 없다.
- Router·분류값 누락·복수 cluster 종류 충돌은 임의 분류하지 않고 제외·집계한다.
- 기존 `SYS.PRINTER`는 기능 CI 성격이며 모델·제조사·시리얼·RAM을 담지 못하므로
  물리 Printer 본체 분류로 사용하지 않는다.
- 물리 Printer는 전용 ACTCI·CI 분류와 속성 세트를 설계한 뒤 구현한다.
- 별도의 `SYS.PRINTER` 기능 CI는 이번 본체 수집 범위에 포함하지 않는다.
- Maximo 기준정보는 분류·속성·MAS UI 등록 및 검증 절차를 먼저 설계하고 별도 승인 없이 변경하지 않는다.

## 2. 1차 — Device 통합과 Switch Actual CI

**완료 결과:** `DeviceCiIntegrate`·`DeviceSource`로 교체했고, `CiSourceFilter.COMPUTER`는
변경하지 않은 채 본체용 `DEVICE` 필터와 cluster 집계를 추가했다. 전체 Gradle 테스트가 성공했다.
D42 읽기 전용 재검증은 `.68` 36행/고유 PK 36개, `.35` 72행/고유 PK 72개이며
각 서버의 Switch 2대가 단일 cluster·단일 `network_kind=Switch`와 대표 MAC을 가졌다.
기존 Computer·VM 34 / 70행의 공통 반환 필드는 확장 전 결과와 행 단위로 동일했다.
Maximo 업무 데이터·기준정보 적재는 수행하지 않았다.

### 범위

- `ComputerCiIntegrate`를 `DeviceCiIntegrate`로 교체한다.
- `ComputerSource`를 `DeviceSource`로 교체한다.
- `ComputerCiIntegrateTest`를 `DeviceCiIntegrateTest`로 교체한다.
- 기존 Computer·VM의 본체와 18개 스펙, 단위, NULL 조건, VM_ID 조건을 그대로 유지한다.
- Device 본체 전용 후보 조건을 추가하되 `CiSourceFilter.COMPUTER`는 변경하지 않는다.
- `CiClassification.GENERIC_SWITCH`를 추가한다.
- Switch는 기존 `ComputerSpec`의 공통 하드웨어 속성을 재사용한다.
- 필요한 경우 Switch 전용 스펙으로 `GENERICCOMPUTERSYSTEM_GENERICTYPE`만 추가한다.
- Switch 대표 MAC은 기존 기본 포트가 아니라 cluster 연결 경로에서 얻는다.
- IP와 전체 Interface는 본체 단일 스펙으로 압축하지 않는다.
- Printer·Router·미판별 네트워크 장비는 이 단계에서 적재하지 않는다.

### 조회·판정

```text
physical network device
  ← view_netport_v1.second_device_fk
cluster device
  → details.fw_device_type
```

포트 행으로 본체가 증폭되지 않도록 물리 장비별 cluster와 종류를 먼저 집계한다.
제품 조회에는 최소한 다음 진단값을 포함한다.

- `cluster_pk`
- `network_kind`
- `network_kind_count`
- 연결 cluster 수
- 판정 충돌 여부

분류 규칙:

```text
현행 physical Computer 조건                    → SYS.COMPUTERSYSTEM
현행 virtual Computer 조건                     → SYS.VIRTUALCOMPUTERSYSTEM
physical network + 단일 network_kind=Switch   → SYS.GENERICSWITCH
Router·종류 누락·복수 종류 충돌                → 미지원, 적재 제외·집계
Network Printer                               → 1차 제외
```

### 매핑 문서화

- `data-mapping/ci/types/device.md`를 Device 통합 매핑 정본으로 사용한다.
  최종 파일명은 `data-mapping/ci/types/device.md`로 한다.
- 실제 Count SQL과 페이지 SQL 전체, 분류 판정, 공통 ACTCI 본체, 공통 스펙,
  분류별 적용 조건과 제외 조건을 이 문서 본문에 작성한다.
- 탐색 쿼리 링크로 제품 매핑 SQL을 대신하지 않는다.
- `computer-run.md`는 구현 명칭과 범위에 맞춰 `device-run.md`로 이동·갱신한다.
- `data-mapping/ci/README.md`의 진입 링크와 유형별 진행표를 갱신한다.
- `design/ci/device.md`에는 선택 이유와 범위만 두고 실제 확정 SQL을 중복하지 않는다.
- `knowledge/`에는 두 서버에서 관측한 수치와 원천 값만 둔다.
- `open-issues.md`에서 Switch 판별은 해결로 내리고 Router 표본·Switch 승격·Printer
  기준정보만 남긴다.

### 완료 기준

- 변경 전후 기존 Computer·VM의 PK·분류·본체·스펙 값이 동일하다.
- `.68`: 기존 Computer·VM 34 + Switch 2 = Device 본체 36건이다.
- `.35`: 기존 Computer·VM 70 + Switch 2 = Device 본체 72건이다.
- 두 서버에서 조회 행 수와 고유 `device_pk` 수가 같다.
- Switch 두 대만 `SYS.GENERICSWITCH`로 판정된다.
- cluster 누락·종류 누락·복수 종류 충돌은 적재되지 않고 진단된다.
- 같은 ACTCINUM을 여러 분류가 처리하지 않는다.
- 전체 자동 테스트가 성공한다.
- 실제 Maximo 적재를 수행했다면 재실행 멱등성과 기존 Computer 분류 충돌 부재를 확인한다.

1차 완료 표기는 Actual CI와 승격을 구분한다.

```text
Device/Computer/VM/Switch 본체·스펙 구현 및 자동 테스트: 완료
Switch Actual CI 실적재: 미실행
Switch CI 대상·속성·승격 범위 설계: 완료, MAS UI 적용·승격 검증 미결
```

## 3. 2차 — Switch 승격과 물리 Printer 기준정보 설계

**완료 결과:** [Device CI 기준정보 설계](../../data-analysis/design/ci/device-reference-data.md)에
MAS UI 입력안을 작성했다. Switch는 신규 `CI.GENERICSWITCH`와 18개 속성,
Printer는 신규 `SYS.PHYSICALPRINTER`·`CI.PHYSICALPRINTER`와 13개 속성을 추천한다.
두 승격 범위는 본체 1:1로 시작하며 Interface·IP·SNMP·기능 Printer는 제외한다.
등록 SQL은 만들지 않았고 MAS UI에도 아직 적용하지 않았다.

### Switch

`SYS.GENERICSWITCH`는 ACTCI 속성 99개가 정상 연결되어 있지만 CITEMPLATE가 0건이다.
기존 기능 분류는 일반 Ethernet Switch 본체에 맞지 않아 `CI.GENERICSWITCH` 신규 분류를 추천한다.

- 기존 CI 분류 재사용 가능성을 대조했다.
- Switch용 `CI.GENERICSWITCH`와 CI에서 유지할 18개 속성을 설계했다.
- `SYS.GENERICSWITCH → CI.GENERICSWITCH` 본체 1:1 승격 범위를 설계했다.
- Interface·SNMP System Group은 Actual CI·관계 구현 전까지 승격 범위에서 제외한다.
- MAS UI 등록 순서와 실제 승격·UI 확인 절차를 작성했다.

### Printer

물리 Printer용 ACTCI·CI 분류를 함께 설계한다. 추천 ID는
`SYS.PHYSICALPRINTER`·`CI.PHYSICALPRINTER`이며, 기존 `SYS.PRINTER`를 본체 분류로 재사용하지 않는다.

공통 장비 속성 후보:

- 이름, 모델, 제조사, 시리얼, UUID, 자산번호
- RAM, 장비 유형, 가상 여부
- 대표 MAC

Printer 전용 속성 후보:

- 용지함 수
- 펌웨어 이름과 버전

CPU·DPI·최대 용지 크기는 현재 원천 근거가 부족하거나 대응 필드가 없어 이번 속성 세트에서 제외한다.

별도 CI 또는 후속 대상으로 유지할 항목:

- IP Address와 Network Interface
- OS/Firmware를 별도 CI로 관리할지 여부
- 토너·드럼 잔량 등 시점성 소모품 정보
- 논리적 `SYS.PRINTER` 기능 CI

기준정보 명세에는 분류 계층, Use With, ACTCI·CI 속성, 승격 범위, 필요한 관계 범위,
표시 순서·필수 여부·단위와 MAS UI 검증·롤백 절차를 포함한다.

이 단계는 UI 입력 설계까지만 수행하고 별도 승인 없이 Maximo 기준정보를 변경하지 않는다.

## 4. 3차 — Printer 기준정보 적용 후 구현

- 승인된 기준정보를 적용하고 조회로 구성을 검증한다.
- `DeviceCiIntegrate`의 Printer 분기를 활성화한다.
- `physicalsubtype='Network Printer'`와 JSON의 Printer 표식을 판정·진단에 사용한다.
- 공통 장비 속성과 승인된 Printer 전용 속성만 적재한다.
- RAM `2.048 GB` 등 원문 값과 단위를 임의 보정하지 않는다.
- 대표 MAC은 비어 있지 않은 후보가 정확히 하나일 때 사용한다.
- ACTCI 적재, 재실행 멱등성, CI 승격, UI 속성 표시를 검증한다.
- 별도 `SYS.PRINTER` 기능 CI는 생성하지 않는다.

Printer 활성화 후 예상 Device 본체 수는 `.68` 37건, `.35` 73건이다.

## 5. 작업 재개 체크리스트

압축이나 세션 전환 후에는 다음 순서로 재개한다.

1. 루트 `CLAUDE.md`와 데이터 분석 `README.md`를 다시 읽는다.
2. 이 계획과 `design/ci/device.md`의 상태를 확인한다.
3. `git status`로 기존 미커밋 조사 문서와 사용자 변경을 확인한다.
4. 완료된 단계와 다음 미완료 단계만 수행한다.
5. Actual CI 구현 완료와 CI 승격 완료를 섞어 기록하지 않는다.
6. Maximo 기준정보는 별도 승인 없이 변경하지 않는다.
