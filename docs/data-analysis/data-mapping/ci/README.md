# CI 데이터 매핑

Device42 개체를 Maximo Actual CI로 적재하는 매핑 정본이다.

> **CI 대상과 대표 Source를 먼저 확정한 뒤 `ACTCI`, `ACTCISPEC`, `ACTCIRELATION`을 매핑한다.**

## 읽는 순서

1. `ci-targets.md` — CI 후보, 중복 View, 포함·제외 범위
2. 대상 확정 후 `ACTCI` 본체 매핑
3. 본체 매핑 후 `ACTCISPEC`, `ACTCIRELATION` 매핑

현재는 `ci-targets.md` 조사 단계다. 세 대상 테이블의 컬럼 매핑 문서는 대상
범위가 확정된 뒤 작성한다.

## Target

| 역할 | Maximo 테이블 |
| --- | --- |
| CI 본체 | `MAXIMO.ACTCI` |
| CI 분류별 속성 | `MAXIMO.ACTCISPEC` |
| CI 간 관계 | `MAXIMO.ACTCIRELATION` |

## 원칙

- View 하나를 CI 유형 하나로 간주하지 않는다.
- 같은 개체를 표현하는 범용 View와 전용 View는 대표 Source 하나로 합친다.
- 본체, 속성, 관계, 비대상을 구분한다.
- 포함·제외가 결정되지 않은 후보는 `../../open-issues.md`에 둔다.
