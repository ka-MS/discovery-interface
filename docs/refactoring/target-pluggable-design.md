# D42 고정 원천과 교체 가능한 타겟 설계

## 확정 요구사항

원천은 D42 고정, 실행당 타겟 하나, 타겟은 설정으로 작업은 기존 CLI 인자로 선택한다.
설정 생략은 maximo, 미등록 타겟은 실행 전 오류, 미등록 CLI 인자는 기존처럼 무시한다.
인자 순서·중복 실행·실패 경계·수집 범위·저장 의미는 보존한다.
실제 두 번째 타겟은 구현하지 않고 테스트 전용 타겟으로 교체 가능성을 검증한다.

## 책임과 의존

| 영역 | 소유 책임 | 허용 내부 의존 |
| --- | --- | --- |
| runtime | 순차 실행·페이지 반복·최소 타겟 작업 계약 | runtime |
| source.device42 | D42 기술 접근·조회·원천 모델·관계 사실 | source.device42 |
| target.maximo | 타겟 DTO·저장·정의 조회 | target.maximo |
| pipeline.d42maximo | 수집 정책·변환·식별자·준비·Job 조립 | runtime, source, target, 자체 pipeline |
| cli | 설정 선택·기존 인자 실행 | runtime |

단일 Gradle 모듈을 유지한다. 원천은 타겟 DTO·관계 코드·식별자 규칙을 모른다.
조회와 원천 모델은 기존 기능별 구분을 유지하되 source로 옮기고, Mapper/Import/Job은 pipeline으로 옮긴다.
기존 device42 기술 클래스는 source.device42로, maximo는 target.maximo로 이동한다.
범용 모델·매핑 DSL·상속 프레임워크·추가 재시도/트랜잭션은 도입하지 않는다.

## 분리 원칙

- D42 조인·원천 사실 해석은 Source, 타겟 값 생성은 Pipeline 매핑.
- 수집 조건은 Pipeline이 선택하며 Query는 SQL로 적용한다. 사후 필터 전환 금지.
- 관계의 원천 식별자 쌍과 타겟 식별자·관계 코드를 분리한다.
- CI 본체와 관계가 같은 식별자 규칙을 사용한다.
- 관계별 고정 접두어를 제거해도 문자열 PK의 정렬을 유지한다. COUNT/중복/배열의 모든 쌍을 보존한다.
- 지원 작업은 타겟이 제공하는 Job 맵 자체이며 별도 기능 목록을 중복 관리하지 않는다.
- 선택한 타겟만 준비한다. 미선택 타겟의 데이터소스 자동 설정도 차단해야 한다.

## 검증

기준 커밋과 전체 경로를 대조하고 기존 테스트 의미를 보존한다. 변경 SQL은
원천 조회 + Mapper의 결과 동등성과 순서로 검증한다. 타겟 선택·비활성화는 실제
Spring 설정 경로를 사용한 테스트로 검증한다. 테스트 전용 타겟은 운영 산출물에 포함하지 않는다.
모의/H2 검증은 운영 D42/DB2 검증을 대신하지 않는다.

## 구현 결정

- `integration.target` 생략은 maximo, 그 외 값은 대소문자 구분한 등록 ID다.
- Application은 TargetModule 구현 설정만 탐색한다. 각 모듈은 조건부 활성화와 자신이 필요한
  컴포넌트·원천 조회·데이터소스를 조립한다. JobRunner는 활성 모듈이 정확히 하나이며 선택 ID와
  일치하는지 검사한 뒤 해당 Job 맵만 사용한다.
- D42 Query는 일반 객체다. 수집 조건을 생성자에 명시적으로 전달하며 Maximo 기본값을 내장하지 않는다.
- `DeviceSelection`의 구조화된 조건 생성과 `FilesystemFilter`를 사용한다. 자유 SQL 설정/DSL은 없다.
- 카탈로그의 기본값 적용은 DISTINCT/ORDER BY보다 앞서 있어, 제거하면 건수와 페이지가 달라진다.
  기준정보의 보충 이름 행도 UNION의 중복 제거·COUNT에 포함된다. 이런 경우 연계부가 값 정책을
  제공하고 SQL이 연산을 수행하도록 전달한다. 값 선택은 원천 소유가 아니며 사후 필터로 바꾸지 않는다.
  카탈로그의 반환 모델에는 동치 그룹 대표값 NULL을 사용하고 최종 UNKNOWN은 Mapper가 생성한다.
- 타겟 테이블 이름이 붙은 조회는 DeviceQuery/SoftwareCatalogQuery/InstalledSoftwareQuery 및
  ManufacturerNamesQuery/OperatingSystemNamesQuery/ProcessorModelsQuery/AdapterModelsQuery로 바꿨다.
  기준/변형의 동일 조회는 공유하되 여덟 Import 작업과 실행 순서는 보존한다.
- Printer MAC의 UPPER 표현은 Mapper로 옮기고 SQL UPPER와의 결과 동등성을 H2로 검증했다.
- 기존 CI 본체가 읽지 않던 Device source_id/system_type/is_virtual 투영은 제거했다.
  사용 중인 타겟 식별자는 전부 Pipeline에서 생성한다.
- 관계 PK는 CAST(... AS varchar)를 유지하고 출발/도착 PK 문자열로 정렬한다.
  일곱 각 조회는 끝점별 접두어가 고정이므로 기존 접두어 연결 정렬과 같은 순서를 갖는다.
- 운영 타겟은 Maximo 하나다. AlternativeTarget은 테스트 소스에만 있으며 다른 작업 이름,
  다른 DeviceSelection, 실제 CpuQuery 행 읽기와 독자적인 적재 모델로 교체 계약을 검증한다.
