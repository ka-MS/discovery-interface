# DB 스키마

이 프로젝트가 소유한 테이블의 DDL 정본이다. 조사·분석 문서는
`docs/data-analysis/` 에 있고, 여기에는 실행 가능한 스키마 정의만 둔다.

## 구성

| 경로 | 대상 |
| --- | --- |
| `discovery/` | `DISCOVERY` 스키마. 이 프로젝트의 ETL 전용 |

`MAXIMO` 스키마는 이 프로젝트가 소유하지 않는다. 적재 대상일 뿐이므로 DDL 을
두지 않는다.

## 파일 규칙

- `NNN-<대상>.sql` 형식. 번호는 적용 순서다.
- 파일 하나가 테이블 하나를 정의한다. 인덱스는 같은 파일에 둔다.
- 이미 적용된 파일은 수정하지 않는다. 변경이 필요하면 다음 번호로 새 파일을
  만든다.
- 실행 결과가 사람 눈에 남아야 하므로 `DROP` 은 여기 두지 않는다.

## 적용 이력

| 파일 | 대상 | 적용 |
| --- | --- | --- |
| `discovery/001-source-target-map.sql` | `DISCOVERY.SOURCE_TARGET_MAP` | 2026-08-28 |

## 애플리케이션은 이 파일을 실행하지 않는다

Spring Boot 의 `spring.sql.init` 은 기본값이 `embedded` 라 Db2 같은 외부
데이터소스에는 동작하지 않는다. 클래스패스 최상단이 아니라 `db/` 아래에 두어
`schema.sql` 자동 실행 규칙과도 겹치지 않는다. 적용은 사람이 한다.

## 주의

같은 디렉터리의 `src/main/resources/application.yaml` 에는 실제 DB 자격정보가
있다. git 추적 대상이 아니지만 `.gitignore` 에 등재된 것도 아니어서,
`git add src/main/resources/` 나 `git add -A` 로 딸려 들어갈 수 있다.
`.gitignore` 에 넣어두는 편이 안전하다.
