# Computer CI 실행 준비

구현: `CiIntegrationJob → ComputerCiIntegrate`.
매핑 정본: [Computer](computer.md).

## 실행 전 준비

1. Maximo의 두 ACTCI Computer 분류와 선택한 스펙을 준비한다.
   BIOS 날짜용 `COMPUTERSYSTEM_BIOSRELEASEDATE`는 현재 환경에 없으므로
   [등록 SQL](../../../../../src/main/resources/db/maximo/ci-computer-bios-release-date.sql)을 검토·별도 실행한다.
   SQL은 한 Db2 compound statement로 작성됐으며 구분자는 `@`다.
   배치는 기준정보를 자동 생성하지 않는다. 이 개발 과정에서 등록 SQL을 실행하지 않았다.
2. 기존 외부 `config/application.yaml`에 아래 설정을 지정한다.
   계정·언어는 실제 Maximo 운영값을 사용하고 시간대는 Target timestamp 저장 기준으로 선택한다.

```yaml
ci:
  change-by: <Maximo_ETL계정>
  lang-code: <Maximo_언어코드>
  zone-id: <저장_시간대>
  page-size: 100
```

환경변수 `CI_CHANGE_BY`, `CI_LANG_CODE`, `CI_ZONE_ID`, `CI_PAGE_SIZE`로도 전달할 수 있다.
시간대 예시는 `Asia/Seoul`, `UTC`다. 첫 세 설정에는 기본값이 없다.
`page-size` 기본값은 100, 허용 범위는 1~1000이다.
CI 설정은 CI 실행 시 확인하므로 다른 작업의 실행을 막지 않는다.

3. 준비 후 `./run.sh ci` 또는 `./gradlew bootRun --args="ci"`로 실행한다.
   이 명령은 실제 업무 테이블을 변경한다. 원천은 외부 접속 설정이 가리키는 D42 한 대다.

## 구현된 흐름

- 실행 시작 시 분류·선택 스펙의 타입·ACTCI 적용 설정·단위 코드 검증.
  미등록·중복·자료형 불일치가 있으면 D42 수집 전에 중단한다.
- `device_pk > 마지막 처리 PK`와 LIMIT로 순서대로 조회한다. 실행 중 총 행 수를 고정하지 않는다.
- 한 번 조회한 ComputerSource로 본체와 스펙을 매핑한다.
- 장비 한 대당 본체 저장과 스펙 저장을 같은 Maximo 트랜잭션으로 처리한다.
- ACTCINUM으로 기존 ACTCIID를 찾고, 신규만 `ACTCISEQ`의 NEXT VALUE를 사용한다.
  스펙도 속성·ACTCINUM·NULL 섹션을 포함한 키로 기존 ID를 찾고 신규만 `ACTCISPECSEQ`를 사용한다.
- 시퀀스 반환값 한 개만 사용하며 그 사이 ID를 임의로 발급하지 않는다.
  현재 두 시퀀스의 증가분은 1000이므로 ID에 간격이 생긴다. MAXSEQUENCE·AUTOKEY를 직접 수정하지 않는다.

## 현재 동작과 정책 경계

이번 구현은 정상 값의 등록·갱신 경로다. 미결 정책을 처리하는 별도 프레임워크는 추가하지 않았다.

- 값이 있는 선택 스펙만 저장한다. 비어 있는 스펙은 신규 생성·기존 값 삭제를 하지 않는다.
- 본체의 이름·설명은 원천 값을 그대로 갱신한다. 숫자·문자열을 임의 반올림·절단하지 않는다.
- LASTSCANDT가 없거나 시간대를 파싱할 수 없으면 실패한다. 실행 시각으로 대체하지 않는다.
- 미지원 단위·길이 초과·분류 변경·DB 오류는 예외를 전달한다. 실패한 장비는 롤백되며 작업은 중단한다.
  앞서 완료된 장비는 이미 커밋되어 있다. 재시도·자동 삭제·분류 변경 정리는 구현하지 않았다.
- CHANGEBY·LANGCODE는 실행 설정, CHANGEDATE는 저장 시각이다. 발견 시각은 설정 시간대로 변환한다.
- 기존 본체의 GUID·CCIDISGUID·HASLD 등 이 매핑이 갱신하지 않는 필드는 유지한다.
  신규 본체는 HASLD=0이며 GUID·CCIDISGUID는 설정하지 않는다.
- 관계·승격은 이번 구현 범위가 아니다.

## 검증

2026-09-14 기준:

- H2의 Db2 모드에서 물리·가상 분류, 템플릿·부모 참조, 단위·BIOS·코어 값,
  재실행 ID 유지, NULL 섹션 키, 신규/갱신 롤백, 사전 정의 검증, 페이지 처리를 테스트했다.
- 코드의 D42 조회 SQL을 .68 / .35에서 읽기 전용으로 실행했다.
- 코드의 분류·스펙 조회 SQL과 등록에 필요한 컬럼·시퀀스를 Maximo에서 읽기 전용으로 확인했다.
- 실제 Maximo의 업무 행 적재, 등록 SQL 실행, UI·승격, Maximo 애플리케이션과의 동시 채번은 미검증이다.
