# OS·Disk·Filesystem·IP CI 매핑 구성 설계

작성일: 2026-09-15

## 1. 목적

Computer에 이어 OS·Disk·Filesystem·IP 네 유형의 Actual CI 매핑을 구성한다.
각 유형의 ACTCI 분류를 정하고, 수집할 스펙을 정하고, 매핑 문서를 작성한다.

Computer가 밟은 경로를 그대로 따른다. 원천 조사 → Maximo 분류·스펙 조사 →
수집 구성안 → 확정 매핑 순이며, 각 산출물의 위치와 형식은
[데이터 분석 README](../../data-analysis/README.md)의 작성 규칙을 따른다.

## 2. 범위

**포함**

- 네 유형의 D42 원천 보강 조사와 Maximo ACTCI 분류·스펙 조사
- 유형별 수집 구성안 (`design/ci/<유형>.md`)
- 유형별 확정 매핑 (`data-mapping/ci/types/<유형>.md`)
- 관계(ACTCIRELATION) 구성 **추천안**과 근거
- `open-issues.md`, `data-mapping/ci/README.md`, `exploration-queries/README.md` 갱신

**제외**

- 적재 구현 코드. enum 추가, `CiIntegrationTask` 구현, 테스트 모두 이번 범위가 아니다.
- Maximo 기준정보 등록·변경. 분류·속성·관계 정의를 만들거나 고치지 않는다.
- Maximo 업무 테이블 적재. 실제 ACTCI 행을 쓰지 않는다.
- 관계의 확정 매핑. 추천안까지만 쓴다.

## 3. 합의된 결정

2026-09-15 사용자 합의다.

| 항목 | 결정 |
| --- | --- |
| 관리 단위 | 네 유형 모두 **독립 ACTCI**. Computer 스펙으로 흡수하지 않는다 |
| 근거 | 장비당 개수가 파일시스템 5.3, IP 1.5, 디스크 1.3개다. ACTCISPEC 키가 `(ACTCINUM, ASSETATTRID, SECTION)`이라 다중 개체를 Computer 스펙으로 담을 수 없다. OS는 관측상 1:1이지만 OS 자체의 EOS 관리 대상이 되도록 독립 CI로 맞춘다 |
| 관계 취급 | `design/ci/<유형>.md`에 **추천안**으로 쓴다. 확정 매핑 표에 넣지 않는다 |
| 구현 | 이번 범위 밖 |

### 밝히고 가는 가정

사용자가 확인했다.

- **스펙 선택** — Computer 선례대로 `CI.*` 기존 분류 스펙을 대조 기준으로 놓고, 원천 대응이 있는 것만 채택한다.
- **식별자** — `D42:<개체종류>:<원천PK>`. Computer의 `D42:DEVICE:<pk>`와 같은 규칙이다.
- **배열 유형** — 원천 PK 하나당 CI 하나다. 여러 Computer에 걸리면 CI는 1개, 관계가 N개다.
- **수집 대상 범위** — Computer 연결분과 전체를 **둘 다 집계**한 뒤 `design/`에서 정한다.

## 4. 작업 순서

```
1단계  조사        네 유형 일괄
2단계  설계        OS 형식 확정 → 나머지 3개
3단계  매핑        OS 형식 확정 → 나머지 3개
4단계  색인·미결   진행표·이슈·쿼리 목록 갱신
```

### 1단계 — 조사 (`knowledge/`)

| 대상 | 산출물 | 내용 |
| --- | --- | --- |
| D42 원천 | Computer 연관 범위 내 관측은 `knowledge/device42/computer-inventory.md` 보강, 그 범위를 넘는 관측은 `knowledge/device42/ci-component-inventory.md` 신규 | 유형별 전체 컬럼, 원천 PK 유일성, 값 보유율, **Computer 외 장비 연결분** |
| Maximo 분류 | `knowledge/maximo/ci-component-classifications.md` 신규 | 네 유형의 ACTCI 분류 후보, 스펙·자료형·단위, `CLASSUSEWITH`·`CLASSSPECUSEWITH` 적용 설정 |
| 관계 규칙 | 위 문서에 포함 | 분류 확정 후 분류쌍으로 `RELATIONRULES` 조회 |

네 유형을 한 번에 조사한다. 쿼리 한 번에 나오므로 쪼개지 않는다.
D42는 **두 서버 모두** 조회하고 어느 서버 관측인지 함께 적는다.
재사용 가치가 있는 쿼리는 `exploration-queries/`에 등록한다.

### 2단계 — 설계 (`design/ci/os.md`, `disk.md`, `filesystem.md`, `ip.md`)

유형마다 분류 선택안과 이유, 스펙 대조표, 식별자 규칙, 관계 추천안,
수집 대상 범위를 쓴다. 추천과 확정을 구분해 표기한다.

OS를 먼저 써서 형식을 잡되 승인을 기다리며 멈추지 않는다. 6절 마지막 행을 따른다.

### 3단계 — 매핑 (`data-mapping/ci/types/os.md` 등 4건)

범위·조회 SQL, 본체 표(5열), 속성 표(6열). 형식은 README의 CI 매핑 예외 절을 따른다.
공통 컬럼 정의를 복사하지 않고 `actci.md`·`actcispec.md`를 참조한다.
관계는 확정 표에 넣지 않고 `design/` 링크만 둔다.

### 4단계 — 색인·미결

`data-mapping/ci/README.md` 진행표에 네 유형 행 추가,
`open-issues.md` ISSUE-8·11 갱신, `exploration-queries/README.md` 목록 추가.

## 5. 유형별 조사 항목

분류명은 전부 추정이다. 실제 존재 여부는 조사로 확인한다.

| 유형 | 원천 | 분류 후보 | 확인할 것 |
| --- | --- | --- | --- |
| OS | `view_deviceos_v1` + `view_os_v1` → Vendor | `SYS.OPERATINGSYSTEM` 계열 | `device_fk` 직접 연결. 관측상 1:1이 항상 성립하는지 |
| Disk | `view_part_v1` type_name='Hard Disk' + `view_partmodel_v1` | `SYS.DISKDRIVE` 계열 | `pcount`가 수량인지 개체 수인지. Part PK가 디스크 한 개 단위인지 |
| Filesystem | `view_mountpoint_v2` | `SYS.FILESYSTEM` / `SYS.LOCALFILESYSTEM` 계열 | `device_fks` **배열**. 용량 단위 |
| IP | `view_ipaddress_v2` + `view_subnet_v1` | `SYS.IPADDRESS` / `SYS.IPINTERFACE` 계열 | `device_fks` **배열**. `netport_fk` 유무에 따른 처리 |

`device_fks` 배열 두 유형은 조인 시 같은 원천 PK가 여러 행이 되어 MERGE 키를 깨뜨린다.
`DISTINCT ON`으로 하나만 남긴다. 근거는 ISSUE-9.

뷰는 높은 버전을 쓴다. `_v2`가 있으면 그쪽이다. 확인은
`exploration-queries/device42/view-version-probe.sql`로 한다.

## 6. 자율 실행 판단 규칙

사용자 부재 중 진행한다. 아래 상황에서 멈추지 않고 규칙대로 처리하고, 판단 근거를 문서에 남긴다.

| 상황 | 처리 |
| --- | --- |
| ACTCI 적용 분류 후보가 **정확히 1개** | 그 분류로 3단계 매핑 문서까지 작성한다 |
| 후보가 **여러 개** | `design/`에 대조표와 추천 1개를 근거와 함께 쓴다. 매핑 문서는 작성하지 않고 이유를 남긴다 |
| 후보가 **0개** | 관측 사실로 기록하고 ISSUE-11에 건다. 매핑 문서는 작성하지 않는다. 분류를 새로 만들자고 제안하되 SQL은 쓰지 않는다 |
| 분류는 있으나 `CLASSUSEWITH`에 ACTCI 적용이 없음 | 후보 0개와 같게 처리한다. 적용 설정 부재를 명시한다 |
| 원천 값은 있는데 대응 속성이 없음 | 미대응으로 표에 남긴다. Computer의 BIOSRELEASEDATE 선례대로 추가 속성 후보로만 적고 등록 SQL은 쓰지 않는다 |
| 속성은 있는데 원천 값이 전건 비어 있음 | 채택하지 않는다. 관측 건수와 함께 이유를 남긴다 |
| `RELATIONRULES` 일치 **0건** | 관측 사실로 기록하고 ISSUE-11에 건다. 관계 추천안은 "사용 가능한 규칙 없음"으로 쓴다 |
| 두 D42 서버 관측이 크게 다름 | 양쪽 수치를 모두 적는다. 한쪽으로 결론내지 않는다 |
| 원천 PK 유일성이 깨짐 | 관측 사실로 기록하고 식별자 규칙을 재검토해 `design/`에 남긴다 |
| 수집 대상 범위 | Computer 연결분과 전체를 모두 집계하고 `design/`에 추천 1개를 근거와 함께 쓴다 |
| OS 문서 형식 승인 대기 | **멈추지 않는다.** OS 형식으로 네 개를 모두 쓰고, 형식 수정 요청이 오면 일괄 반영한다 |

### 하지 않는 것

- Maximo 쓰기 일체. 조사는 읽기 전용 실행기만 쓴다.
- 기준정보 등록 SQL 작성·실행.
- 적재 구현 코드 작성.
- 분류·관계를 확정으로 단정하는 서술. 근거가 조사 결과 하나뿐이면 추천으로 쓴다.
- 추정치를 관측치처럼 적기. 수치는 실행 결과에서 옮긴다.

## 7. 산출물

| 파일 | 구분 |
| --- | --- |
| `knowledge/device42/` 원천 보강 | 신규 또는 갱신 |
| `knowledge/maximo/` 분류·스펙 조사 | 신규 |
| `design/ci/os.md`, `disk.md`, `filesystem.md`, `ip.md` | 신규 |
| `data-mapping/ci/types/os.md`, `disk.md`, `filesystem.md`, `ip.md` | 신규. 6절 규칙에 따라 일부 보류 가능 |
| `exploration-queries/device42/`, `maximo/` 추가 쿼리 | 신규 |
| `data-mapping/ci/README.md`, `exploration-queries/README.md`, `open-issues.md` | 갱신 |

커밋은 조사·설계·매핑을 분리한다. 접두어는 `docs:`를 쓴다.

## 8. 검증

- 문서에 적는 모든 수치는 실행기로 실제 조회한 결과다. 출력은 `local/db-access-kit/work/` 아래에만 두고 커밋하지 않는다.
- D42 조사는 `.68`과 `.35` 양쪽에서 실행한다.
- 등록한 재사용 쿼리는 실제로 실행해 동작을 확인한 것만 남긴다.
- 코드 변경이 없으므로 빌드·테스트 대상이 아니다. 코드에 손대야 할 이유가 생기면 범위를 벗어난 것이므로 멈추고 보고한다.

## 9. 미결 연결

- 관리 단위·포함 범위: [ISSUE-8](../../data-analysis/open-issues.md)
- 분류·속성·관계·식별자 정책: ISSUE-11
- 배열 조인 중복: ISSUE-9
