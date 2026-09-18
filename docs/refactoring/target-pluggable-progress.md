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
- [x] 전체 Query/모델/매핑 및 정책 결합 감사, 설계 확정.
- [x] source/target/pipeline 패키지 이전과 원천 독립화.
- [x] CI 관계 원천·매핑 분리 및 본체와 식별자 규칙 공유.
- [x] 설정 기반 단일 타겟 조립, 비선택 타겟 초기화 차단.
- [ ] 기존 회귀 및 테스트 전용 대체 타겟·의미 동등성 검증.
- [ ] 문서·아키텍처 검사·전체 test/bootJar·최종 리뷰·로컬 커밋.

## 조사와 결정

- 관계 Query는 타겟 DTO를 직접 생성하며 SQL에서 타겟 식별자를 생성한다.
- Device CI SQL의 식별자/고정값 일부는 이미 Mapper에서 계산하고 있어 조회에서 제거 가능한 중복이다.
- 소프트웨어 카탈로그 SQL의 UNKNOWN 기본값도 타겟 표현이므로 매핑으로 이동한다.
- 수집 범위는 바꾸지 않는다. 정책 소유권 분리와 SQL 실행 위치를 구분한다.
- 관계 페이지는 PK의 문자열 정렬을 유지한다. 숫자 정렬로 바꾸지 않는다.

## 다음 작업

카탈로그·기준정보 SQL 투영의 정책 전달을 검증하고, 문서 정본 갱신과 전체 회귀·산출물 감사를 진행한다.

## 구현 체크포인트 1

- 176개 기존 클래스의 source/target/pipeline 책임별 이전. Mapper/Writer/Import 실행 본문은 최대한 유지.
- 원천 Query를 자동 스캔하지 않고 `MaximoQueries`가 명시적인 원천 조건으로 생성한다.
- `DeviceSelection`은 D42 조건의 SQL 표현, `MaximoSourcePolicy`는 실제 수집 값 선택을 소유한다.
- 관계는 `Device42Relation`/`RelationSource` → `CiRelationMapper` → Writer로 분리했다.
- `MaximoCiIdentity`를 여섯 CI 본체 Mapper와 관계 Mapper가 공유한다.
- `integration.target` 기본값 maximo. `TargetModule` 계약을 구현한 조건부 조립부만 탐색한다.
- 전역 DataSource 자동 설정을 제외하고 선택된 Maximo 조립부에서만 가져온다.
- 기존 131 테스트에 타겟 선택 테스트 4개를 추가하여 135개 전체 통과.
- 추가 표적 검증: `./gradlew test --tests '*SourceSqlParityTest' --tests '*TargetSelectionTest'` 중 초기 Printer 정규화 공백 차이를 수정했고 두 클래스가 각각 통과했다.
- `SourceSqlParityTest`: 기준 커밋에서 동결한 27 Query + 7 관계 COUNT/PAGE SQL 전체 대조 통과. 정책 차이는 별도 실행 검증.
- `./gradlew test --tests '*EquivalenceTest'` 성공: 장비 조건 10,800 조합의 SQL 3값 논리, 파일시스템 제외/대소문자/NULL, 일곱 관계의 식별자·코드·문자열 정렬·중복·NULL·페이지 경계.
- 검증은 H2/Mockito이며 운영 D42·DB2에 접속하지 않았다.
- 삭제한 이전 CiSourceFilter/FilesystemSelection은 이번 변경으로 대체된 규칙이며 Git 이력으로 복구 가능하다.
