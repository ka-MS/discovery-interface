# Device42 → Maximo 데이터 분석

Device42(원천)와 Maximo(타겟) 사이의 매핑 작업 문서다.

## 읽는 순서

수집 범위는 [사업 추진 범위](../requirements/business-scope.md)를 먼저 확인한다. 원문에 명시된 수집 항목과 독립 CI 대상 여부를 구분한 뒤 아래 문서를 읽는다.

1. `knowledge/maximo/deployedasset-model.md` — 타겟 구조와 ASSETCLASS 판별자
2. `knowledge/device42/views.md` — 원천 뷰와 device 연결 키
3. `knowledge/device42/servers.md` — **조사 전 필수.** Device42 서버가 둘이고 성격이 다르다
4. `data-mapping/README.md` — 라우팅과 진행 현황
5. `data-mapping/<패키지>/<테이블>.md` — 테이블별 매핑

수집 구성·관리 단위를 검토할 때는 해당 `design/` 문서도 확인한다.
Computer 초기 설계와 관계 구성안은 [Computer 수집 설계](design/ci/computer.md)에 보존한다.
현재 Computer·VM·Switch 통합 구현과 Printer 후속안은 [Device 통합 수집 설계·대조표](design/ci/device.md)에 있다.
MAS UI에 등록할 Switch 승격·물리 Printer 분류와 속성은 [Device CI 기준정보 설계](design/ci/device-reference-data.md)에 있다.
OS·Disk·Filesystem·IP는 [os](design/ci/os.md)·[disk](design/ci/disk.md)·[filesystem](design/ci/filesystem.md)·[ip](design/ci/ip.md)에 같은 구성으로 둔다.

## 조사 대상 서버

Device42 는 두 대이고 수집 범위가 다르다. **한 대만 보고 결론을 내지 않는다.**

| 서버 | 넓은 원천 |
| --- | --- |
| 192.168.2.68 | 소프트웨어, 파트, 마운트 |
| 192.168.1.35 | 네트워크, OS |

같은 뷰라도 건수가 크게 다르다. 예를 들어 `view_part_v1` 은 192.168.2.68 에서
479건, 192.168.1.35 에서 86건이다. 한쪽 수치를 절대값으로 옮겨 적으면 안 된다.

전환 방법과 커버리지 표는 `knowledge/device42/servers.md` 에 있다.

## 구성

| 디렉터리 | 내용 |
| --- | --- |
| `knowledge/` | 관측 사실. 수치는 스냅샷이며 상단에 관측 시점과 재조회 쿼리를 명시한다 |
| `design/` | 수집 구성안·대조표·선택 이유. 검토안과 결정된 내용을 구분 |
| `data-mapping/` | 테이블 단위 매핑 정본 |
| `exploration-queries/` | 조사·검색용 재사용 쿼리. 매핑 SQL의 정본이 아님 |
| `tools/` | 매핑 문서 골격 생성 |
| `open-issues.md` | 미결·정책 대기 항목의 상태·남은 결정과 설계 링크 |

접속 수단은 `local/db-access-kit/` 에 있다. git 추적 대상이 아니다.

## 작성 규칙

> **짧게, 중복 없이, 관측 사실·설계안·미결 상태·확정 매핑을 구분한다.**

- 관측 사실은 `knowledge/`, 구성안·대조표·선택 이유는 `design/`, 확정된 필드 매핑·변환·SQL은 `data-mapping/`에 둔다.
- `open-issues.md`는 미결 항목의 상태·남은 결정과 설계 링크를 관리한다. 상세 설계안·대조표를 중복 작성하지 않는다.
- 같은 사실을 반복하지 않는다. 결론에 필요한 수치만 남긴다.
- 테이블·컬럼은 한글명을 함께 적고, Source 와 조건은 실행 가능한 식으로 쓴다.
- **실제 매핑 SQL은 매핑 문서 본문에 작성한다. 재사용 쿼리의 링크·파일명·블록명으로 대신하지 않는다.**
- pk 리터럴에 의존하지 않는다. 원천 pk 는 재수집 시 바뀐다.
- 수치를 담는 문서는 상단에 관측 스탬프를 단다.

## 컬럼 매핑 표

Target 테이블의 컬럼 표는 7열 고정이다. 앞 4열은 Maximo 메타데이터에서 생성한다.

| 열 | 출처 |
| --- | --- |
| Target 컬럼 | `MAXATTRIBUTE.ATTRIBUTENAME` |
| 한글명 | `L_MAXATTRIBUTE.TITLE` (LANGCODE='KO') |
| 타입 | `MAXATTRIBUTE.MAXTYPE` + `LENGTH` + `SCALE` |
| Null | `MAXATTRIBUTE.REQUIRED` 반전 |
| 구분 | 분석. 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결 |
| Source | 분석. Device42 뷰.컬럼 |
| 변환·조건 | 분석. 변환식, 기본값, 조건 |

골격 생성:

```bash
bash local/db-access-kit/scripts/run-maximo.sh \
  docs/data-analysis/exploration-queries/maximo/column-skeleton.sql \
  local/db-access-kit/work/maximo
bash local/db-access-kit/scripts/run-maximo.sh \
  docs/data-analysis/exploration-queries/maximo/table-description.sql \
  local/db-access-kit/work/maximo
python3 docs/data-analysis/tools/gen-mapping-skeleton.py
```

설계 문서는 `../superpowers/specs/2026-08-27-data-analysis-docs-design.md` 다.

## CI 매핑 문서 예외

CI는 여러 원천 유형이 같은 Target 테이블에 들어오므로 문서와 구현 클래스를 1:1로 묶지 않는다.

- `data-mapping/ci/actci.md`, `actcispec.md`, `actcirelation.md`: Target별 공통 규약.
  전체 컬럼·타입·Null은 기존 7열 표에 한 번만 둔다. Source에는 Maximo 참조와 유형 문서도 쓸 수 있다.
- `data-mapping/ci/types/<유형>.md`: 범위·조회 SQL·분류·본체 필드·속성·관계 매핑.
  공통 컬럼 정의를 복사하지 않는다. 구분 값은 기존 목록을 사용한다.
- 유형별 본체 표: `Target 컬럼 | 한글명 | 구분 | Source | 변환·조건`.
- 유형별 속성 표: `ASSETATTRID | 한글 의미 | 값 컬럼 | 구분 | Source | 변환·조건`.
  분류를 표 앞에 명시하며, 행은 물리 컬럼이 아닌 ACTCISPEC 속성 한 건에 대응한다.
- 유형 간 관계는 출발 유형 문서 한 곳에서 정의하고 다른 유형 문서는 링크한다.
- 분류를 가정한 대응안은 확정 매핑 표에 넣지 않는다. 분류·스펙 대조표와 수집 구성안은 `design/ci/<유형>.md`, 미결 상태·남은 결정은 `open-issues.md`에 둔다.
- 원천·분류·속성·관계 조회 SQL은 해당 매핑 문서 본문에 둔다. `exploration-queries/`와 연결하지 않는다.
