# Device42 → Maximo 데이터 분석 문서 체계 설계

작성일: 2026-08-27

## 1. 목적

Device42(원천)와 Maximo(타겟) 사이의 데이터 매핑 작업에서 발생하는 산출물을
세 가지로 분리해 축적한다.

- 세션이 바뀌어도 이전 조사 결과를 이어받을 수 있어야 한다.
- 관측 사실과 매핑 설계를 섞지 않는다.
- 재사용 가능한 탐색 쿼리만 남기고 일회성 쿼리는 남기지 않는다.

## 2. 범위

포함: `docs/data-analysis/` 하위 문서 체계, 문서 템플릿, 작성 규칙, 초기 구축 범위.

제외: 매핑 내용 자체의 확정, ETL 코드 변경, 정책 결정.

## 3. 디렉터리 구조

```
docs/data-analysis/
├── README.md                       진입점 - 문서 지도, 읽는 순서, 진행 상태
├── open-issues.md                  미결 및 정책 대기 항목
│
├── knowledge/                      관측 사실 (스냅샷)
│   ├── device42/
│   │   ├── doql-constraints.md     DOQL 실행 제약
│   │   ├── views.md                실재 뷰 목록, 컬럼, device 연결 키
│   │   ├── device-types.md         type/subtype 체계와 분포
│   │   └── servers.md              서버별 데이터 성격과 소스 커버리지
│   └── maximo/
│       ├── deployedasset-model.md  NODEID 계층, ASSETCLASS 판별자
│       └── tables.md               DPA* 목록, 컬럼, 노드 커버리지
│
├── data-mapping/                   매핑 정본
│   ├── README.md                   ASSETCLASS 라우팅, 실행 순서, 진행 현황
│   ├── asset/
│   │   ├── deployedasset.md
│   │   ├── dpacomputer.md
│   │   ├── dpaos.md
│   │   ├── dpacpu.md
│   │   ├── dpadisk.md
│   │   ├── dpalogicaldrive.md
│   │   ├── dpanetadapter.md
│   │   ├── dpatcpip.md
│   │   ├── dpamediaadapter.md
│   │   ├── dpanetdevice.md
│   │   ├── dpanetprinter.md
│   │   ├── dpadisplay.md           원천 없음
│   │   └── dpaswsuite.md           원천 없음
│   └── software/
│       └── dpasoftware.md
│
├── exploration-queries/            재사용 탐색 쿼리 (git 추적)
│   ├── README.md
│   ├── device42/
│   └── maximo/
│
└── tools/
    └── gen-mapping-skeleton.py     메타데이터 → 매핑 문서 골격 생성
```

저장소 루트의 `CLAUDE.md` 에서 `docs/data-analysis/README.md` 를 진입점으로
안내한다. 새 세션이 자동으로 읽는 파일은 `CLAUDE.md` 뿐이므로, 1절의 세션
연속성 요구는 이 연결로 충족한다.

`data-mapping/` 하위 폴더는 Java 패키지 구조를 따른다.

| 문서 | 구현 |
| --- | --- |
| `data-mapping/asset/deployedasset.md` | `integration/asset/DeployedAssetIntegrate.java` |
| `data-mapping/asset/dpaos.md` | `integration/asset/DpaOsIntegrate.java` |
| `data-mapping/software/dpasoftware.md` | `integration/software/DpaSoftwareIntegrate.java` |

## 4. 계층 경계

| 계층 | 위치 | 담는 것 | 담지 않는 것 |
| --- | --- | --- | --- |
| 접속 | `local/db-access-kit/` | 자격정보, 인증서, 실행기, 접속 확인용 smoke 쿼리 | 조사 쿼리, 조사 결과 |
| 탐색 쿼리 | `docs/data-analysis/exploration-queries/` | 재사용 가능한 조회 쿼리 | 실행 결과 |
| 관측 사실 | `docs/data-analysis/knowledge/` | 구조, 분포, 제약 | 매핑 설계, 판단 |
| 매핑 설계 | `docs/data-analysis/data-mapping/` | 테이블/컬럼 매핑, 조회조건 | 원천 구조 설명 |
| 미결 | `docs/data-analysis/open-issues.md` | 정책 대기 항목 | 해결된 항목 |
| 생성 도구 | `docs/data-analysis/tools/` | 문서 골격 생성 스크립트 | 분석 내용 |

`local/db-access-kit/`에는 접속 수단만 남긴다. 조사 쿼리는 `exploration-queries/`로
이동하고 실행기가 해당 경로를 인자로 받는다.

예외는 `queries/device42-smoke.sql`과 `queries/maximo-smoke.sql` 두 개다.
`check-connections.sh`가 이 경로를 하드코딩해 접속 확인에 쓴다. 분석 쿼리가
아니라 실행기의 일부이므로 키트에 남긴다.

## 5. 작성 규칙

### 5.1 PK 비의존

원천 데이터는 재수집 시 pk가 바뀐다. 문서와 쿼리는 pk 리터럴에 의존하지 않는다.

실행기는 바인드 파라미터를 지원하지 않는다. Device42 실행기는 SQL을 그대로
전송하고(`run-device42.sh`), Db2 실행기는 `PreparedStatement`를 값 주입 없이
실행한다(`Db2ReadOnlyQuery.java`). `:uuid` 같은 명명 파라미터는 실행되지 않는다.

대상 지정은 두 가지만 쓴다.

| 방식 | 용도 |
| --- | --- |
| 조건식 | 분류 기준으로 여러 건을 잡을 때 |
| `target` CTE | 특정 장비 한 대를 잡을 때. 쿼리 최상단 한 줄만 수정한다 |

```sql
-- 금지: pk 리터럴
WHERE device_fk = 83

-- 금지: 실행기가 지원하지 않는다
WHERE d.uuid = :uuid

-- 허용: 조건식
WHERE d.virtualsubtype_id = 11

-- 허용: target CTE. 대상 변경은 이 한 줄만 고친다
WITH target AS (
    SELECT device_pk AS pk FROM view_device_v2 WHERE name = 'episode'
)
```

### 5.2 관측 스탬프

건수와 분포는 시점 의존 값이다. 해당 수치를 담는 문서는 상단에 관측 시점, 대상
서버, 재조회 쿼리 경로를 명시한다.

```
> 관측 2026-08-26 · Device42 192.168.1.35
> 재조회 exploration-queries/device42/device-type-distribution.sql
```

### 5.3 톤

- 관측된 값과 매핑 규칙만 기술한다. 판단과 의견은 `open-issues.md`에 둔다.
- 같은 사실을 반복하지 않는다. 결론에 필요한 수치만 남긴다.
- 테이블·컬럼은 한글명을 함께 적고, Source와 조건은 실행 가능한 식으로 쓴다.

### 5.4 탐색 쿼리 등록 기준

`exploration-queries/`에는 다음을 만족하는 쿼리만 둔다.

- 파라미터화되어 재실행 가능하다.
- 특정 조사 1회로 끝나지 않는다.
- 파일명이 목적을 설명한다.

## 6. 문서 템플릿

### 6.1 매핑 문서

```markdown
# <TARGET_TABLE>

<MAXOBJECT 한글 설명 한 줄. 예: 배치된 자산 컴퓨터 운영 체제 - Device의 운영체제 상세>

> Target: MAXIMO.<TABLE> · ASSETCLASS: <CLASS> · 구현: <Class>.java

## 1. 관계
- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : <1|N> <TABLE>
- 선행: <선행 테이블>

## 2. 테이블 매핑
| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |

## 3. 조회 조건
| 조건 | 식 | 사유 |
| --- | --- | --- |

## 4. 컬럼 매핑
| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |

## 5. 조회 쿼리
3번 조건이 반영된, Device42에서 원천을 끌어오는 실행 가능한 SELECT.

## 6. 미결
`open-issues.md`의 이슈 ID와 한 줄 요약만 둔다.
```

최상단 한 줄 설명은 `MAXIMO.MAXOBJECT` / `L_MAXOBJECT`(LANGCODE='KO')에서 가져온다.

3번은 조건과 사유를 설명하고, 5번은 그 조건이 반영된 실행 가능한 쿼리를 둔다.
두 항목은 항상 일치해야 한다.

6번에는 이슈 내용을 옮겨 적지 않는다. `open-issues.md`가 미결의 정본이며,
매핑 문서는 이슈 ID와 한 줄 요약만 참조한다. 내용을 양쪽에 두면 상태가
어긋난다. 이슈 ID는 `ISSUE-<번호>` 형식이다.

카디널리티는 테이블마다 다르므로 템플릿에서 확정하지 않는다. 대상 테이블의
MERGE 키로 판단한다. 키가 `NODEID` 단독이면 노드당 1행이므로 `1:1`이다.

`DEPLOYEDASSET`은 계층의 루트이므로 `1. 관계`에서 부모와 선행 항목을 생략하고,
대신 적재 대상 필터와 키 전략을 기술한다.

### 6.2 컬럼 매핑 표 스키마

열 구성은 7열로 고정한다. 고정 스키마여야 엑셀 산출물을 생성할 수 있다.

| 열 | 내용 | 출처 |
| --- | --- | --- |
| Target 컬럼 | Maximo 컬럼명 | `MAXATTRIBUTE.ATTRIBUTENAME` |
| 한글명 | 한글 컬럼명 | `L_MAXATTRIBUTE.TITLE` (LANGCODE='KO') |
| 타입 | 타입·길이·소수자리 | `MAXATTRIBUTE.MAXTYPE` + `LENGTH` + `SCALE` |
| Null | Y/N | `MAXATTRIBUTE.REQUIRED` 반전 |
| 구분 | 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결 | 분석 |
| Source | Device42 뷰.컬럼 또는 `-` | 분석 |
| 변환·조건 | 변환식, 기본값, 조건 | 분석 (`DEFAULTVALUE` 포함) |

앞 4개 열은 Maximo 메타데이터에서 생성한다. 분석으로 채우는 열은 뒤 3개다.
대상 컬럼 목록은 `SYSCAT.COLUMNS`가 아니라 `MAXATTRIBUTE`를 기준으로 한다.
`ROWSTAMP` 같은 시스템 컬럼이 제외되어 매핑 대상만 남는다.

`MAXATTRIBUTE.DEFAULTVALUE`가 있는 컬럼은 구분을 `상수` 후보로 본다. 예를 들어
`DPAOS.MANUFACTURER`와 `DPAOS.NAME`은 `REQUIRED=1`, `DEFAULTVALUE='UNKNOWN'`이다.

타입 표기는 `SCALE`까지 포함한다. `SCALE`이 0이면 `MAXTYPE(LENGTH)`, 0이 아니면
`MAXTYPE(LENGTH,SCALE)`로 적는다. 대상 테이블에는 `DECIMAL(10,2)` 컬럼이
8개 테이블에 13개 있다. `SCALE`이 없으면 반올림 자리수를 복원할 수 없다.

`구분` 값과 기존 엑셀 v14 분류의 대응은 다음과 같다.

| 본 문서 | 엑셀 v14 |
| --- | --- |
| 직접 | 직접·변환 |
| 변환 | 직접·변환 |
| 상수 | 상수 |
| 채번 | 채번·시스템 |
| 원천없음 | 원천 없음 |
| 미결 | 결정·검증 필요 |

v14의 `직접·변환`을 `직접`과 `변환`으로 분리한다. 변환식 유무가 구현 난이도를
가르므로 구분한다.

## 7. 엑셀 산출물

`docs/data-analysis/`가 정본이다. 엑셀은 컬럼 매핑 표를 파싱해 생성한다.
고정 7열 스키마가 이를 보장한다. 생성 스크립트는 본 설계 범위에 포함하지 않는다.

## 8. 이슈 처리 방침

| 이슈 | 처리 |
| --- | --- |
| SOURCEID가 서버 간 불일치 | 정책 사항, `open-issues.md`에 기록 |
| 스위치 cluster/physical 레코드 분리 | `open-issues.md`에 기록 |
| PDU가 COMPUTER로 분류됨 | 수집 정책에 반영 |
| 가상 장비 vendor 없음 | 이슈 아님. UNKNOWN이 정상 결과 |

## 9. 초기 구축 범위

조사 완료분으로 내용을 채운다.

- `knowledge/device42/` 4개 문서
- `knowledge/maximo/` 2개 문서
- `data-mapping/README.md` 라우팅 및 진행 현황
- `open-issues.md`
- `exploration-queries/` 기존 쿼리 이전 및 파라미터화
- Maximo 메타데이터 조회 쿼리 2건 등록 (테이블 설명, 컬럼 골격)

매핑 문서 14장은 템플릿 골격과 확인된 항목(관계, 카디널리티, 소스 뷰)까지만
채우고, 컬럼 매핑은 대상 테이블을 하나씩 정해 진행한다.

원천이 없는 것으로 확인된 `DPADISPLAY`, `DPASWSUITE`도 문서를 만든다. 해당
문서는 원천 없음과 그 사유를 기록한다. 문서가 없으면 미조사와 구분되지 않는다.

## 10. 미결

- 엑셀 생성 스크립트의 구현 시점과 형태
