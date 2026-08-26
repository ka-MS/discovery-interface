# discovery-interface

Device42 에서 수집한 자산 정보를 Maximo 로 적재하는 Spring Boot 연동
애플리케이션이다.

## 데이터 분석 문서

원천·타겟 구조와 매핑은 `docs/data-analysis/` 에 있다. 매핑 관련 작업을
시작하기 전에 `docs/data-analysis/README.md` 를 먼저 읽는다.

## DB 접속

접속 수단은 `local/db-access-kit/` 에 있다. git 추적 대상이 아니며 실제
자격정보를 포함한다. 폴더 내용을 응답, 로그, 커밋에 옮기지 않는다.
실행 규칙은 `local/db-access-kit/AGENTS.md` 를 따른다.

조회 쿼리는 `docs/data-analysis/exploration-queries/` 에 있다. 실행 결과는
`local/db-access-kit/work/` 아래에만 둔다.

## 작업 규칙

- 원천 pk 에 의존하는 코드나 문서를 만들지 않는다. Device42 `device_pk` 는
  수집 서버와 재수집 시점에 따라 바뀐다.
- Maximo 는 읽기 확인만 한다. 쓰기가 필요하면 대상과 복구 방법을 확인한 뒤
  진행한다.
