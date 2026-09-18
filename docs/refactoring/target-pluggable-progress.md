# 타겟 교체 구조 리팩터링 진행 기록

## 목표와 기준선

- 목표: D42 고정 원천과 타겟별 저장·매핑·조립을 분리하고 설정으로 한 타겟을 선택한다.
- 시작 브랜치: `refactor/integration-structure`.
- 기준 커밋: `8cabf92030d882e732613e07f493de41de800b61`.
- 작업 브랜치: `codex/refactor-target-pluggable` (기준 HEAD에서 생성).
- 사용자 미추적 `docs/data-analysis/exploration-queries/device42/webwas-ci-source.sql` 보존, 커밋 제외.
- 기준 검증: WSL Ubuntu에서 `./gradlew test bootJar --rerun-tasks` 성공 (7 tasks executed).
- 운영 DB 접속·적재·DDL, 접속 파일 변경, push/merge/PR은 하지 않는다.

## 정본과 기록

- 현재 구조 설계 정본: `integration-structure-design.md` (최종 구조로 갱신).
- 이번 상세 설계: `target-pluggable-design.md`.
- 이전 완료 이력: `integration-structure-progress.md` 보존.
- 이번 진행 이력: 이 문서. 설계·검증·커밋 단위로 갱신한다.

## 체크포인트

- [x] 지침·기준선 확인, 새 브랜치 생성, 기준 test/bootJar 실행.
- [ ] 전체 Query/모델/매핑 및 정책 결합 감사, 설계 확정.
- [ ] source/target/pipeline 패키지 이전과 원천 독립화.
- [ ] CI 관계 원천·매핑 분리 및 본체와 식별자 규칙 공유.
- [ ] 설정 기반 단일 타겟 조립, 비선택 타겟 초기화 차단.
- [ ] 기존 회귀 및 테스트 전용 대체 타겟·의미 동등성 검증.
- [ ] 문서·아키텍처 검사·전체 test/bootJar·최종 리뷰·로컬 커밋.

## 조사와 결정

- 관계 Query는 타겟 DTO를 직접 생성하며 SQL에서 타겟 식별자를 생성한다.
- Device CI SQL의 식별자/고정값 일부는 이미 Mapper에서 계산하고 있어 조회에서 제거 가능한 중복이다.
- 소프트웨어 카탈로그 SQL의 UNKNOWN 기본값도 타겟 표현이므로 매핑으로 이동한다.
- 수집 범위는 바꾸지 않는다. 정책 소유권 분리와 SQL 실행 위치를 구분한다.
- 관계 페이지는 PK의 문자열 정렬을 유지한다. 숫자 정렬로 바꾸지 않는다.

## 다음 작업

전체 조회와 테스트를 감사하고 구체적인 정책 전달·타겟 조립 계약을 확정한다.
