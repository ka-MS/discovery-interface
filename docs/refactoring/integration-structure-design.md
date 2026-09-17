# 연계 구조 리팩토링 설계

## 목표와 범위

현재 Device42 → Maximo CLI의 동작을 보존하면서 작은 실행 코어, 연계별 조회·매핑,
독립된 타겟 저장으로 분리한다. 단일 Gradle 모듈과 `com.itmsg.device42` 루트는 유지한다.
새 기능, Spring Batch, 의존성 업그레이드, 운영 DB 적재는 범위 밖이다.

## 책임과 의존 방향

| 패키지 | 책임 | 허용 의존 |
| --- | --- | --- |
| `runtime` | 작업 순차 실행, 기존 페이지 범위 반복 | JDK, 로깅 |
| `device42` | 연결과 DOQL/JDBC 실행 기술 | JDK, Spring |
| `maximo` | 타겟 DTO, Writer, MERGE, 정의 로딩·스냅샷 | JDK, Spring |
| `integration/d42maximo` | 수집 SQL·반환 모델·매핑·식별자·Job 조립 | 위 세 영역 |
| `cli` | 기존 인자에서 Job 실행 | runtime |

하위 기술/타겟 영역에서 연계로 역참조하지 않는다. 기능별 코드는 같은 패키지에
모으며, 공통 조회 모델이나 범용 Map 컨텍스트를 만들지 않는다. 인터페이스/상속은
현재 공통 실행 계약에 필요한 만큼만 사용한다.

## 이전 대응

| 기존 | 새 소유자 |
| --- | --- |
| IntegrationJob / JobRunner | runtime / cli |
| 영역별 Job와 Task 루프 | 연계별 Job + runtime TaskSequence |
| Device42ConnectionFactory / Device42DatabaseConfig | device42 |
| MaximoDatabaseConfig / dto.maximo | maximo |
| Asset·Conversion·Software Integrate의 putData와 MERGE | maximo 영역별 Writer |
| 각 Integrate의 조회와 원천 DTO | 연계의 기능별 Query·반환 모델 |
| 각 Integrate의 변환 | 연계의 기능별 Mapper 또는 작은 메서드 |
| CI Writer·DefinitionLoader/Cache | maximo.ci (정의 ID 목록은 호출자가 전달) |
| CI enum·SpecMapper·수집 조건 | integration.d42maximo.ci |
| 관계 enum과 원천 SQL | 연계의 관계 정의 (공통 관계 실행 유지) |
| 실행 클래스에 있던 공유 식별자·제외 조건 | 연계별 작은 규칙 클래스 |

실제 클래스 이름/대응표는 구현에 맞춰 이 문서에서 갱신한다.

## 보존할 동작

- 기존 CLI 이름, 인자 순서, 알 수 없는 이름 무시, CI 관계 단독/중복 호출 의미.
- 기존 작업 순서, 작업 실패 후 계속 실행, CI 공통 준비 실패 전파, 본체 실패 후 관계 실행.
- 각 조회의 COUNT, 정렬, LIMIT/OFFSET, 빈 페이지 이후에도 count 기준 반복.
- SQL 의미와 바인딩 순서·자료형, 식별자·MERGE 키·시퀀스·중복/관계 처리.
- NULL, 단위, 시간대, 페이지별 변경 시각 생성과 건별 변환/저장 실패 범위.
- 본체 성공/스펙 실패의 부분 적재 및 현재 auto-commit 의미. 트랜잭션을 추가하지 않는다.
- 기존 조회/매핑/적재 집계. 적재 건수만으로 새 상태나 exit code를 정의하지 않는다.

## 설계 결정

- 연계 전용 SQL과 원천 모델은 integration에 둔다. D42 문법을 사용해도 수집 범위는 연계 정책이다.
- 페이지 반복과 작업 순차 실행만 추출한다. JobPlan DSL·의존성 DAG·운영 기능은 만들지 않는다.
- 기존 배치 단위 매핑과 오류 경계를 유지한다. 간단한 변환에 전용 인터페이스를 만들지 않는다.
- CI 정의는 매 실행 생성한 읽기 전용 값이며 작업 객체에 실행 상태를 보관하지 않는다.
- SQL text block은 소유 클래스 가까이에 유지한다. SQL 리소스 로더 도입은 하지 않는다.

## 검증과 완료 기준

1. CPU Asset·OS CI 시범 전환 후 모든 Asset/CI/관계/Conversion/Software 경로를 이전한다.
2. 기존 테스트의 검증 의미를 유지하고 오류·페이징·조립·패키지 경계 테스트로 보강한다.
3. 기준 커밋과 SQL·바인딩·매핑·실행 동작을 대조한다.
4. 최종 전체 테스트를 실제 실행하고 빌드를 확인한다. H2/Mock은 실제 DB 검증을 대신하지 않는다.
5. 임시 어댑터·옛 중복 경로·금지 의존·패키지 순환이 없어야 한다.
6. 매핑 문서의 구현 참조, README, CLAUDE.md를 최종 구조와 일치시킨다.
7. 진행 기록과 로컬 커밋, 최종 리뷰를 완료한다. push/merge/PR은 하지 않는다.
