# DPAMSOFTWARE / DPAMSWVARIANT

소프트웨어 변환 대상·변환 변형 (적재하지 않음)

> Target: 없음. 적재 대상이 아니라 다른 문서와 달리 두 테이블을 한 장에 둔다.

> 관측 2026-08-28 · Maximo BLUDB
> 재조회 `../../exploration-queries/maximo/dpa-view-conversion-requirements.sql`

## 1. 요구하는 뷰

**없다.** 다른 도메인과 달리 UI 뷰가 이 변환 대상를 조인하지 않는다.
`DPACSOFTWARE` 는 `TLOAMSOFTWARE` 를 경유해 제조사만 검사한다. 근거는
`../software/tloamsoftware.md` 참조.

따라서 이 변환 대상에 값을 넣지 않아도 `DPASOFTWARE` 의 UI 노출에는 영향이 없다.

**결정: 현재 목적에서는 등록하지 않는다.** 컴퓨터 소프트웨어 UI 노출만
기준으로 하면 `TLOAMSOFTWARE` + `DPAMMANUVARIANT` + `DPASOFTWARE.TLOAMSOFTWAREID`
셋으로 충분하다. Maximo 의 전체 정규화 체계까지 맞출 때 이 두 테이블이 필요해진다.

`DPACSOFTWARE` 뷰 텍스트에 `dpamswvariantid` 라는 이름이 나오지만 이는
`t800.tloamsoftwareid` 에 붙은 컬럼 별칭이며 테이블 조인이 아니다. 소프트웨어
도메인에서는 `TLOAMSOFTWARE` 가 변환 변형 역할을 대신한다.

## 2. 테이블 구조

| 테이블 | 컬럼 | 타입 | Null | 비고 |
| --- | --- | --- | --- | --- |
| `DPAMSOFTWARE` | `SOFTWAREID` | BIGINT(19) | N | PK. `DPAMSOFTWARESEQ`(START 1987) |
| | `SOFTWARENAME` | ALN(256) | N | 유일 인덱스 |
| | `VALIDATED` | YORN(1) | N | 관측 전건 `0` |
| | `COMPLIANCESETTING` | ALN(32) | Y | |
| `DPAMSWVARIANT` | `DPAMSWVARIANTID` | BIGINT(19) | N | PK. `DPAMSWVARIANTSEQ`(START 2069) |
| | `SOFTWARENAME` | ALN(256) | N | 변환 대상의 정규명 |
| | `SOFTWAREVARIANT` | ALN(256) | N | 유일 인덱스 |

양쪽 1,982행이고 전건 `SOFTWARENAME = SOFTWAREVARIANT` 다.

`DPASOFTWARE.SOFTWARENAME` 은 기존 수집분 13,031행이 전건 이 변환 대상에 등록되어
있다. 수집 도구가 이름을 함께 등록한 결과다.

## 3. Device42 원천과의 관계

Device42 카탈로그(`view_software_v1`)와 이름이 거의 겹치지 않는다.

| | 카탈로그 | 변환 대상과 일치 |
| --- | --- | --- |
| `.35` | 808종 | 1종 |
| `.68` | 2,273종 | 1종 |

Device42 는 스캐너가 주워온 원시 문자열을 쓴다
(`/usr/bin/mysql  Ver 8.0.34 for Linux on x86_64 (MySQL Community Server - GPL)`).
변환 대상은 사람이 정리한 제품명이다(`SQL Server 2000`).

`view_softwareinuse_v1.alias_name`(관측 674/4900)이 변환 변형과 같은 개념이라
정규화를 시작할 때 원천으로 쓸 여지가 있다.

`DPAMSOFTWARE`와 `TLOAMSOFTWARE` 사이에 FK나 UI 뷰의 직접 조인은 없다. 다만
기존 데이터의 값은 연결되어 있다. DPAMSOFTWARE 이름 1,982종이 TLOAMSOFTWARE의
서로 다른 이름 1,982종과 전부 일치하고, TLOAMSOFTWARE 2,175행의 이름도 전건
DPAMSOFTWARE에 존재한다.

## 4. 미결

- 현재 범위에서는 두 테이블을 적재하지 않는다.
- 라이선스 준수나 이름 통합 기능을 범위에 넣을 경우 원시 문자열 2,268종을 그대로
  등록할지 별칭을 정규화할지 결정해야 한다.
