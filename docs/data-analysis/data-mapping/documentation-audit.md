# 제출용 매핑 문서 정합성 점검

## 기준과 역할

- 점검일: 2026-09-21.
- 코드 기준 HEAD: `f72ffa070179b1f32e3e7d9449b838b92e65e030`.
- 작업 브랜치: `codex/refactor-target-pluggable` (최종 확인 완료).
- 현재 코드가 구현 사실의 원천이다. 문서는 제출용이며 후속 가이드·운영 문서의 근거로 사용한다.
- 관측 수치·등록 ID는 관측 당시 스냅샷이다. 코드와 과거 관측만으로 현재 운영 DB 상태를 단정하지 않는다.
- 이 정비는 문서·문서 검증 도구만 변경한다. 운영 코드·기준정보·접속 설정은 변경하지 않는다.
- 사용자 변경 `docs/data-analysis/design/ci/relations.md`와 미추적 `exploration-queries/device42/webwas-ci-source.sql`은 이번 정비 대상에서 제외한다.

## 명세의 정본

| 대상 | 정본 | 책임 |
| --- | --- | --- |
| CI 분류·스펙 | [classstructure.md](ci/classstructure.md) | 코드 분류 12개, 스펙 43행/고유 42개, 조회·유효성 규칙, 환경 등록정보와 구분 |
| CI 관계 | [relations.md](ci/relations.md) | 관계도·일곱 코드 대응·원천 키·COUNT/PAGE·분류쌍 저장 가드 |
| CI 공통 저장 | [ACTCI](ci/actci.md), [ACTCISPEC](ci/actcispec.md), [ACTCIRELATION](ci/actcirelation.md) | 컬럼·바인딩·시퀀스·MERGE·부분 실패 |
| 유형·테이블 매핑 | [매핑 인덱스](README.md) | 실제 원천 조회·필드 변환·Writer SQL |
| 후보·정책 | [설계](../design/ci/relations.md), [미결 목록](../open-issues.md) | 실행 명세와 구분한 선택 이유·후속 범위 |

## 수정 내용

- 공통 CI 문서의 과거 ‘채번·필수값·MERGE 미정’을 실제 구현으로 교체했다.
- CLASSSTRUCTURE·CLASSSPEC·적용 설정과 코드가 선택하는 속성 전체를 통합했다.
- 관계를 원천 연결 사실 → 타겟 식별자·코드 → 저장 가드로 추적할 수 있게 했다.
- 분산된 관계 PAGE SQL을 통합 문서로 옮기고 실제 COUNT도 함께 기록했다.
- Device 정의 캐시 조회의 불완전한 별도 SQL을 공통 로더 원문으로 일원화했다.
- Database Instance 문서에서 조사용 넓은 조회를 실행 SQL과 혼동하는 설명을 정리했다.
- Instance HOME NULL 스펙, 문자열 trim, IP USES 경로, Filesystem 제외 조건, 관계 구현 상태를 바로잡았다.
- DEPLOYEDASSET.SYSTEMROLE=ASSETCLASS, DPACPU.CURRSPEED=0.00, NetAdapter PORT trim 후 절단을 명시했다.
- OS·CPU 변환 조회는 UNKNOWN을 상수로 보충하지 않는다는 실제 동작과 UI 조인 제약을 명시했다.
- DPATCPIP의 시퀀스·자연키 예외, 자산·소프트웨어 Writer의 INSERT/UPDATE·미사용 컬럼을 보강했다.
- PDU 제외를 모든 자식에 일반화한 설명을 수정했다. DPANETDEVICE의 현재 조회에는 별도 PDU 제외식이 없다.
- 독립 Database와 과거 CI 후보 조사를 현행 구현과 분리 표시했다.
- CLAUDE.md에 ‘제출용 정본·후속 문서의 근거’라는 유지 규칙을 기록했다.

## 검증 기록

문서 정비와 로컬 검증을 완료했다. 운영 코드 변경 없이 코드 기준의 명세를 정리했다.

| 검증 | 실제 결과 |
| --- | --- |
| MappingDocumentationTest·SourceSqlParityTest 강제 재실행 | BUILD SUCCESSFUL; 구현 참조·기존 원천 SQL 동등성 검사 |
| check_mapping_contract.py | PASS: Writer MERGE 24개, PAGE 30개, 관계 COUNT 7개, 정의 조회 3개 |
| 분류·스펙·관계 커버리지 | PASS: 분류 12개, 스펙 43행/고유 42개, 관계 7개 |
| 로컬 링크·매핑 관련 앵커 | PASS: 524개 링크 및 매핑 관련 앵커; 과거 제목 링크는 호환 앵커 유지 |
| check_target_baseline.py --docs | PASS: 본문 보존 검사, 문서 링크 521개, PAGE SQL 30개 |
| 전체 test bootJar --rerun-tasks | BUILD SUCCESSFUL, 7개 작업 실제 실행. 35개 suite / 테스트 142개 / 실패·오류·건너뜀 0개 |
| git diff --check | 통과 |

전체 테스트는 H2/Mockito·소스 검사이며 운영 DB 적재 검증이 아니다.
기존 unchecked 경고와 JVM class-sharing 경고는 있었으나 테스트·빌드는 성공했다.
프로덕션 Java·설정 파일은 변경하지 않았다. 사용자 요청에 따라 문서 정비와 검증 스크립트를
로컬 커밋으로 기록한다. 사용자 변경 두 파일은 제외하며 push는 수행하지 않는다.

실행 명령:

```bash
./gradlew test --tests '*MappingDocumentationTest' --tests '*SourceSqlParityTest' --rerun-tasks
python3 scripts/refactoring/check_mapping_contract.py
python3 scripts/refactoring/check_target_baseline.py --docs
./gradlew test bootJar --rerun-tasks
git diff --check
```

문서 검사는 정규화한 SQL 전사, enum 목록·관계 코드 대응, 로컬 링크·앵커 커버리지 검사다.
자동 테스트의 H2/Mockito 검증과 운영 DOQL·DB2 검증은 구분한다.
이번 작업에서 운영 DB 접속·적재·DDL·승격·UI 확인은 수행하지 않는다.

## 남은 제약

- 현재 운영 DB의 전체 CLASSSPEC·RELATIONRULES 등록 현황은 다시 조회하지 않았다.
  제출 환경의 등록정보를 별첨하려면 [분류 조회](ci/classstructure.md)와 [관계 조회](ci/relations.md)의 읽기 전용 SQL 결과를 관측 날짜·환경과 함께 확보해야 한다.
- OS·CPU UNKNOWN 보충, 미수집 필드·독립 CI 추가는 기능 변경이다. 문서 정비로 코드 동작을 바꾸지 않았다.
- 과거 실적재 기록은 당시 범위의 증거이며 현재 코드의 재실행·전체 승격 성공을 뜻하지 않는다.
- 자동 문서 검사는 업무 의미를 대신 판정하지 않는다. 필드 의미·NULL·정렬·수집 조건 변경 시 Mapper와 실제 SQL을 다시 대조한다.
