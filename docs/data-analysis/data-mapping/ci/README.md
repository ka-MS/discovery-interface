# CI 데이터 매핑

Device42 개체를 Maximo Actual CI로 적재하는 매핑 정본이다.

> **공통 Target 규약과 유형별 원천 매핑을 분리한다. 같은 규칙은 한 곳에만 둔다.**

## 읽는 순서

1. [CI 모델](../../knowledge/maximo/ci-model.md) — Actual CI와 CI 비교, 참조 경로
2. [CI 분류 모델](../../knowledge/maximo/ci-classification.md) — CLASSSTRUCTURE·CLASSSPEC 컬럼과 적용 범위
3. [ci-targets.md](ci-targets.md) — CI 후보, 중복 View, 포함·제외 범위
4. [ACTCI](actci.md), [ACTCISPEC](actcispec.md), [ACTCIRELATION](actcirelation.md) — 테이블별 컬럼 매핑
5. [Database](types/database.md), [Database Instance](types/database-instance.md) — 유형별 조회·분류·속성·관계

Computer 본체·스펙 매핑은 [Computer](types/computer.md)에서 시작한다.
수집 항목·원천 연결·표본 근거는 [Computer 연관 수집 원천](../../knowledge/device42/computer-inventory.md)에 있다.
실제 분류·스펙·관계 정의는 [Computer 관련 분류 조사](../../knowledge/maximo/computer-classification-specs.md),
수집 대상·스펙 추천안은 [Computer CI 수집 설계](../../design/ci/computer.md)에 있다.

DB·Instance 전체 원천 컬럼과 참조 구조는 [원천 구조](../../knowledge/device42/database-model.md)에 있다.

OS·Disk·Filesystem·IP의 원천 관측은 [OS·Disk·Filesystem·IP 원천 조사](../../knowledge/device42/ci-component-inventory.md),
분류·스펙·관계 규칙은 [OS·Disk·Filesystem·IP 분류 조사](../../knowledge/maximo/ci-component-classifications.md),
수집 구성안은 `../../design/ci/` 의 [os](../../design/ci/os.md)·[disk](../../design/ci/disk.md)·[filesystem](../../design/ci/filesystem.md)·[ip](../../design/ci/ip.md)에 있다.

공통 7열 표는 전체 컬럼·참조 규칙을 소유한다. 유형 문서 본문에는 실제 조회 SQL과
본체·속성 매핑을 작성한다. 관계 SQL과 정의는 출발 유형 문서에만 둔다.

## 유형별 진행

| 유형 | 문서 | 상태 |
| --- | --- | --- |
| Computer | [computer.md](types/computer.md) | 본체·스펙 저장 구현 및 자동 테스트 완료. [실행 준비](types/computer-run.md)의 설정·BIOS 날짜 속성 등록 후 실제 적재·UI 검증 필요 |
| Database | [database.md](types/database.md) | 원천 10컬럼 사용처·SQL 작성; 본체 이름·메모·분류, 이름 속성 대응. 추가 속성·적용 설정·필수값 미결 |
| Database Instance | [database-instance.md](types/database-instance.md) | 원천 9컬럼·Resource 보강·SQL 작성; 본체 이름·메모·분류, 이름·제품명·버전 문자열 속성 대응. 추가 속성·관계·필수값 미결 |
| OS | [os.md](types/os.md) | 매핑 작성 완료. 분류 SYS.OPERATINGSYSTEM, 속성 4개. EOL·EOS 대응 속성 없음. 적재 구현 미착수 |
| Disk | [disk.md](types/disk.md) | 매핑 작성 완료. 분류 DEV.DISKDRIVE, 속성 3개. 제조사·펌웨어·미디어 유형은 원천 전건 비어 있음. 적재 구현 미착수 |
| Filesystem | [filesystem.md](types/filesystem.md) | 매핑 작성 완료. 분류 SYS.FILESYSTEM, 속성 4개. 컨테이너 마운트 선별 미결. 적재 구현 미착수 |
| IP | [ip.md](types/ip.md) | 본체·속성만 작성. 분류 NET.IPADDRESS, 속성 1개. **Computer와 직접 관계 규칙이 없어 경로 결정 필요** |

기준정보 보완과 나머지 정책은 [ISSUE-8·11](../../open-issues.md)에 둔다.
Computer 본체·속성은 구현했다. OS·Disk·Filesystem·IP는 매핑까지 작성했고 적재 구현은 미착수다.
관계는 네 유형 모두 확정하지 않았다. 규칙 관측 결과는 분류 조사 문서에 있다.
실제 Maximo 적재·UI 검증은 전 유형 미완료다.

## Target

| 역할 | Maximo 테이블 |
| --- | --- |
| CI 본체 | `MAXIMO.ACTCI` |
| CI 분류별 속성 | `MAXIMO.ACTCISPEC` |
| CI 간 관계 | `MAXIMO.ACTCIRELATION` |

## 원칙

- CI 수집·매핑은 D42 원천을 기준으로 정의하며, DPA 테이블·적재 결과·변환 규칙에 의존하지 않는다. 공통 원천 사실은 knowledge에서 참조한다.
- View 하나를 CI 유형 하나로 간주하지 않는다.
- 같은 개체를 표현하는 범용 View와 전용 View는 대표 Source 하나로 합친다.
- 본체, 속성, 관계, 비대상을 구분한다.
- 수집 구성 후보·대조표는 `../../design/ci/`에 두고, 남은 포함·제외 결정은 `../../open-issues.md`에서 추적한다.
