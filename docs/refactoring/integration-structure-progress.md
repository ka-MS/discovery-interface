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
- [x] 매핑 문서·README·CLAUDE.md 갱신
- [x] 전체 테스트 실제 실행·빌드·최종 리뷰·완료 기록

## 확인된 기존 제약

- Job의 failures 목록은 반환/exit code에 반영되지 않는다. 기존 동작을 유지한다.
- CI 스펙 실패 후 본체 보존은 기존 테스트가 명시하는 동작이다.
- device-run.md의 Writer 트랜잭션/generated keys 설명은 현재 코드와 맞지 않는다.
  문서를 실제 MERGE·부분 적재 구현에 맞췄고 저장 동작은 바꾸지 않았다.
- README의 computer-run.md 링크, 매핑 문서의 @Order/단일 클래스 참조를 새 구조에 맞췄다.
- CI README의 다섯 유형/미구현 IP 관계 설명은 현재 여섯 유형·일곱 관계와 달랐다.
  현재 구현 안내를 갱신하고 IP의 과거 관측은 날짜와 후속 관계 정본 링크로 구분했다.
- 과거 조사 수치·운영 검증 이력과 docs/superpowers의 당시 설계는 이번 실행 결과로 간주하지 않는다.
  매핑 골격 생성기의 기존 NODEID 시퀀스 안내 등 수집 정책과 무관한 과거 문구는 이번 변경으로 고치지 않았다.
- 실제 운영 D42/Maximo SQL 실행·적재는 이번 검증 범위가 아니다.

## 최종 상태

전체 이전·최종 검증·리뷰를 마쳤다. 이 문서를 포함한 최종 문서/검증 기록 커밋으로 작업을 마무리한다.
범위 내 미완료 작업은 없다. 운영 DB 검증은 아래 명시한 제외 범위이며 별도의 실행 승인이 필요하다.

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

## 문서·최종 리뷰

- CLAUDE.md, README, 데이터 분석 README의 문서-구현 대응 규칙을 Query/매핑/Writer 기준으로 맞췄다.
- 모든 Import·Query·Writer에 매핑 문서 링크를 연결했다. MappingDocumentationTest가 파일 존재와
  전체 흐름의 참조 누락을 검증한다. data-analysis 전체 Markdown의 로컬 링크 검사: 깨진 링크 0.
- 골격 생성기는 존재하지 않는 Integrate 클래스 대신 현재 Writer를 안내한다. 문서 생성/DB 조회는 실행하지 않았다.
- 코드 리뷰: 범용 컨텍스트/상속/등록 프레임워크 없음. Conversion 8개와 NetDevice는 내부 매핑 메서드,
  관계는 일곱 정의와 단일 조회/실행 구현이다. 새 타겟/명령/설정/정책을 추가하지 않았다.
- 이전용 Task 인터페이스와 옛 실행 클래스는 제품 소스에서 제거했다. 제거한 Mapper/Task 코드는 Git 이력으로 복원 가능하다.
  기존 미사용 DTO/접속 검증 주석은 보존했다. 임시 추출 스크립트와 기준선 사본은 ignored build 아래이며 제품/커밋에 포함하지 않는다.

## 최종 검증 결과

- `./gradlew test bootJar --rerun-tasks`: 성공. **131 tests, 30 suites, 실패/오류/skip 0**.
  Gradle 7개 task 모두 실제 실행했다. baseline 114 tests를 유지하고 17개 검증 케이스를 추가했다.
- `python3 scripts/refactoring/check_baseline.py --artifact`: 성공.
  소스 대조 외에 jdeps의 내부 클래스 의존 442개를 분석해 하위 영역 역참조와 패키지 순환이 없음을 확인했다.
  JAR의 애플리케이션 클래스 186개에 옛 패키지/Integrate/IntegrationTask가 없고,
  Import 27개·Query 28개가 포함됨을 확인했다.
  jdeps의 외부 라이브러리 not found 표시는 애플리케이션 출력만 분석했기 때문이며 빌드 실패가 아니다.
- `git diff --check`: 오류 없음.
- JAR: `build/libs/discovery-interface-0.0.1-SNAPSHOT.jar`. 실행하거나 실제 DB에 연결하지 않았다.
- 남은 검증 제약: H2 Db2 모드·Mock·소스/바이트코드 대조는 실제 DOQL/Db2 엔진·운영 데이터 적재를 대신하지 않는다.
  그 운영 검증은 이번 목표에서 명시적으로 제외했으며, 완료된 것으로 주장하지 않는다.

## 완료 기준별 근거

| 기준 | 확인 근거 |
| --- | --- |
| 1. 전체 실행 경로 이전 | 설계의 28개 대응표, JobCompositionTest, 패키징 클래스 개수 |
| 2. 옛/임시 실행 경로 없음 | 원본 클래스 제거, JAR의 옛 패키지/인터페이스 부재 검사 |
| 3. 역참조/순환 없음 | DependencyBoundaryTest와 compiled jdeps 그래프 검사 |
| 4·7. 테스트 유지와 동작 검증 | 114개 기존 테스트 누락 0, 기대값 유지, 131개 전체 통과 |
| 5. 실제 테스트/빌드 | --rerun-tasks의 7개 task 실행, test XML과 bootJar |
| 6. 의미 보존 검토 | 377개 메서드/상수 및 75개 클래스 대조, SQL 식/Statement 종류 별도 검사 |
| 8. 문서 정합성 | MappingDocumentationTest, 전체 로컬 링크 검사, CLAUDE/README/설계 갱신 |
| 9. 누락/과도한 추상화 리뷰 | 명시적 조립, 단순 매핑 내부화, 관계 정의 유지, 금지 도입 없음 |
| 10. 기록/최종 인계 | 이 기록과 목적별 로컬 커밋, 최종 보고의 브랜치/검증/제약 |

## 로컬 커밋

- `42022af` docs: 연계 구조 리팩토링 설계와 기준선 기록
- `396cad8` refactor: CPU 자산과 OS CI의 조회 매핑 저장 분리
- `b034330` refactor: 전체 연계의 조회 매핑 저장 책임 분리
- `292626e` refactor: 실행 코어와 원천 타겟 의존 경계 확정
- 최종 문서·문서 참조 테스트·검증 기록은 `docs: 최종 연계 구조와 검증 기록 정리` 커밋에 포함한다.
  해당 커밋의 ID는 `git log`에서 확인한다 (문서 자신의 커밋 ID를 내용에 삽입하지 않는다).
- 사용자 미추적 SQL 파일은 수정·스테이징하지 않았다. push/merge/PR은 수행하지 않았다.
