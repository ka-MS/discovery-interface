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
- [ ] 실행 코어·CLI·접속 기술 분리
- [ ] Asset 전체 이전
- [ ] CI·관계·정의 스냅샷 전체 이전
- [ ] Conversion·Software 전체 이전
- [ ] 금지 의존/순환·동작 보존 테스트와 기준 코드 대조
- [ ] 매핑 문서·README·CLAUDE.md 갱신
- [ ] 전체 테스트 실제 실행·빌드·최종 리뷰·완료 보고

## 확인된 기존 제약

- Job의 failures 목록은 반환/exit code에 반영되지 않는다. 기존 동작을 유지한다.
- CI 스펙 실패 후 본체 보존은 기존 테스트가 명시하는 동작이다.
- device-run.md의 Writer 트랜잭션/generated keys 설명은 현재 코드와 맞지 않는다.
  문서는 실제 구현에 맞추되 저장 동작은 바꾸지 않는다.
- 실제 운영 D42/Maximo SQL 실행·적재는 이번 검증 범위가 아니다.

## 다음 행동

전체 흐름의 추출은 완료했다. 공통 실행·기술·정의 모델의 최종 소유권을 이전하고,
조립/의존 경계를 검증한다. 아직 전체 목표 완료가 아니다.

## CPU·OS 시범 전환

- CPU를 CpuQuery/CpuMapper/CpuImport와 maximo.asset.DpaCpuWriter로 분리했다.
- OS CI를 OsCiQuery/OsCiMapper/OsCiImport로 분리하고 기존 CI Writer를 사용한다.
- 기존 메서드 본문을 추출해 SQL·바인딩·매핑을 유지했다. 기존 테스트의 기대값은 변경하지 않았다.
- PageLoop는 Iterator 기반 페이지 범위만 제공한다. Import의 지역 집계 변수와 오류 경계를 보존하기 위해
  콜백/범용 상태 객체를 도입하지 않았다.
- `./gradlew test --tests '*CpuImportTest' --tests '*OsCiImportTest' --tests '*DiscoveryInterfaceApplicationTests' --rerun-tasks` 성공.
- 현재 기존 Task 인터페이스와 DTO 패키지는 나머지 이전을 위한 중간 상태다. 전체 이전에서 제거한다.

## 전체 흐름 추출

- Asset 나머지 10개, CI 나머지 5개, Conversion 8개, Software 2개의 조회·매핑·저장을 분리했다.
- Conversion의 단순 변환은 Import 내부 메서드로 유지해 불필요한 Mapper 클래스 8개를 만들지 않았다.
- FilesystemSelection과 SoftwareIdentity를 추출해 실행 클래스 간 공유 규칙 호출을 없앴다.
- `./gradlew test --tests '*asset*' --tests '*DiscoveryInterfaceApplicationTests'` 성공.
- `./gradlew test --tests '*ci*' --tests '*DiscoveryInterfaceApplicationTests'` 성공.
- `./gradlew test` 성공 (compileJava/compileTestJava/test 실제 실행).
- CPU 페이징/오류 흐름과 PageLoop 테스트 추가 검증도 성공했다.
