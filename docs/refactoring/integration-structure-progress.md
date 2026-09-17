# 연계 구조 리팩토링 진행 기록

## 기준선

- 시작일: 2026-09-18
- 기준 브랜치: `feat/db-instance-ci`
- 기준 HEAD: `a9a5ea397761f11e5c4381da5512cfab25f4c05a`
- 작업 브랜치: `codex/refactor-integration-structure` (기준 HEAD에서 생성)
- 사용자 미추적 파일: `docs/data-analysis/exploration-queries/device42/webwas-ci-source.sql`.
  수정·스테이징하지 않는다.
- 기준 테스트: `./gradlew test --rerun-tasks` 성공. 114 tests, 5개 Gradle task 실제 실행.

## 작업 목록

- [x] 지침/참조 문서 확인, 새 브랜치 생성, 설계·진행 기록 작성
- [x] 기준 테스트 확인
- [x] CPU Asset·OS CI 시범 전환과 검증
- [x] 실행 코어·CLI·접속 기술 분리
- [x] Asset 전체 이전
- [x] CI·관계·정의 스냅샷 전체 이전
- [x] Conversion·Software 전체 이전
- [x] 금지 의존/순환·동작 보존 테스트와 기준 코드 대조
- [ ] 매핑 문서·README·CLAUDE.md 갱신
- [ ] 전체 테스트 실제 실행·빌드·최종 리뷰·완료 보고

## 확인된 기존 제약

- Job의 failures 목록은 반환/exit code에 반영되지 않는다. 기존 동작을 유지한다.
- CI 스펙 실패 후 본체 보존은 기존 테스트가 명시하는 동작이다.
- device-run.md의 Writer 트랜잭션/generated keys 설명은 현재 코드와 맞지 않는다.
  문서는 실제 구현에 맞추되 저장 동작은 바꾸지 않는다.
- 실제 운영 D42/Maximo SQL 실행·적재는 이번 검증 범위가 아니다.

## 다음 행동

코드 전체 이전과 소스 대조·회귀 테스트는 완료했다. 문서 참조 검사와 최종 전체 테스트/빌드를
실제로 재실행하고, 최종 리뷰 및 커밋을 마친다. 아직 전체 목표 완료가 아니다.

## CPU·OS 시범 전환

- CPU를 CpuQuery/CpuMapper/CpuImport와 maximo.asset.DpaCpuWriter로 분리했다.
- OS CI를 OsCiQuery/OsCiMapper/OsCiImport로 분리하고 기존 CI Writer를 사용한다.
- 기존 메서드 본문을 추출해 SQL·바인딩·매핑을 유지했다. 기존 테스트의 기대값은 변경하지 않았다.
- PageLoop는 Iterator 기반 페이지 범위만 제공한다. Import의 지역 집계 변수와 오류 경계를 보존하기 위해
  콜백/범용 상태 객체를 도입하지 않았다.
- `./gradlew test --tests '*CpuImportTest' --tests '*OsCiImportTest' --tests '*DiscoveryInterfaceApplicationTests' --rerun-tasks` 성공.
- 시범 전환 당시 남겨둔 기존 Task 인터페이스와 DTO 패키지는 이후 전체 이전에서 제거했다.

## 전체 흐름 추출

- Asset 나머지 10개, CI 나머지 5개, Conversion 8개, Software 2개의 조회·매핑·저장을 분리했다.
- Conversion의 단순 변환은 Import 내부 메서드로 유지해 불필요한 Mapper 클래스 8개를 만들지 않았다.
- FilesystemSelection과 SoftwareIdentity를 추출해 실행 클래스 간 공유 규칙 호출을 없앴다.
- `./gradlew test --tests '*asset*' --tests '*DiscoveryInterfaceApplicationTests'` 성공.
- `./gradlew test --tests '*ci*' --tests '*DiscoveryInterfaceApplicationTests'` 성공.
- `./gradlew test` 성공 (compileJava/compileTestJava/test 실제 실행).
- CPU 페이징/오류 흐름과 PageLoop 테스트 추가 검증도 성공했다.

## 실행·소유권 경계 확정

- runtime/cli/device42/maximo/integration.d42maximo로 모든 기존 경로를 이전했다.
- Job이 구체적인 Import를 NamedTask로 조립한다. 네 영역별 Task 인터페이스·@Order 의존은 제거했다.
- DoqlClient가 JDBC 자원만 관리하고, 연계 Query의 SQL·변환·예외 경계를 유지한다.
- CI 정의 로더는 호출자가 준 ID만 사용한다. 스냅샷은 매 실행 지역 값이며 연계 enum을 참조하지 않는다.
- 관계도 Query/Job/Writer로 분리했지만 기존 일곱 정의 기반 실행은 그대로다.
- 세부 패키지 순환 방지를 위해 공유 조건은 ci.selection, SW 식별자는 software.mapping으로 배치했다.
- 최종 과도한 분리 검토에서 단순 복사뿐인 NetDeviceMapper는 Import의 메서드로 합쳤다.
- 새 경계·CLI·JDBC 수명·작업 조립·실패 격리 테스트를 포함한 `./gradlew test`가 성공했다.

## 기준선 대조

- 기준 커밋을 build/refactoring-baseline에 별도 추출해 Spring scanner 순서를 테스트했다.
  `./gradlew test --tests '*BaselineOrderTest'` 성공. CI 순서는
  DatabaseInstance→Device→Disk→Filesystem→IP→OS이며 새 Job과 일치한다.
  이 임시 테스트는 제품 코드/커밋에 포함하지 않는다.
- 영구 기록한 `python3 scripts/refactoring/check_baseline.py` 성공:
  28개 실행 경로, 377개 메서드/상수, 이동한 모델·규칙·설정 75개 대조, 차이 0.
  기존 114개 테스트 이름/개수 누락 0. 구조 치환만 정규화하며 SQL 리터럴·바인딩·매핑은 유지한다.
  Statement/PreparedStatement 종류와 실제 실행할 SQL 식도 별도 대조했다.
- DependencyBoundaryTest: runtime/device42/maximo/cli 의존 규칙과 세부 Java 패키지 순환 없음 확인.
- JobCompositionTest: 모든 영역의 전체 작업 순서·중복 실행과 CI 정의 전달 확인.
- DoqlClientTest: 자원 종료 순서·조회 실패/close 실패의 suppressed 예외·기존 접속 실패 전파 확인.
- 실제 DB SQL 실행, DDL, 업무 데이터 적재는 하지 않았다.
