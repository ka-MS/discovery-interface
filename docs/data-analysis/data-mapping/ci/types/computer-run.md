# Computer CI 실행 준비

구현: `CiIntegrationJob → CiDefinitionLoader → 공통 캐시 → ComputerCiIntegrate`.
매핑 정본: [Computer](computer.md).

## 실행

- 기존 접속 설정으로 `./run.sh ci` 또는 `./gradlew bootRun --args="ci"`를 실행한다.
  이 명령은 실제 업무 테이블을 변경한다. 원천은 접속 설정이 가리키는 D42 한 대다.
- CI 전용 외부 설정은 없다. 배치 크기는 `DEFAULT_BATCH_SIZE=1000`,
  변경자·언어는 `CHANGE_BY=Device42`·`LANG_CODE=KO` 상수다.
  변경자는 연계 식별 문자열이며 실제 Maximo 사용자 계정인지 검증하지 않는다.
- 변경 시각과 발견 시각의 저장 기준은 JVM 기본 시간대다.
  발견 시각의 원천 오프셋을 해석한 뒤 해당 시간대로 변환한다.
- BIOS 출시일을 수집하려면 ASSETATTRIBUTE에 전역 ALN 속성 `COMPUTERSYSTEM_BIOSRELEASEDATE`를 준비한다.
  현재 미등록이다. 분류 템플릿 없이도 명시적 추가 속성 경로를 사용할 수 있지만,
  화면 표시·승격은 별도 검증 대상이다. 두 분류에 템플릿을 등록하면 기존 템플릿 경로가 우선한다.
  [등록 SQL](../../../../../src/main/resources/db/maximo/ci-computer-bios-release-date.sql)은
  별도 실행용이며 구분자는 `@`다. 이번 수정에서 실행하지 않았다.
  등록 전에는 해당 스펙만 생략한다.

## 처리 흐름

```text
CiIntegrationJob
  CiDefinitionLoader.load → 실행 전용 캐시 생성
  각 CI 작업에 같은 캐시 전달
    getTotalCount
    배치 반복: getData → mapData → putData
```

- `CiClassification` enum에서 조회할 분류를 정한다. enum 추가 시 SQL 조건이 자동으로 확장된다.
- 캐시는 선택한 ACTCI 분류, 그 분류의 전체 스펙, ASSETATTRIBUTE 전체 행의 필요한 컬럼을 담는다.
  정상 구성에서는 분류·속성·스펙 3회 조회하며 개별 CI 작업은 정의 DB를 다시 조회하지 않는다.
  값 적재를 위한 ACTCI·ACTCISPEC 기존 행 조회는 계속 수행한다.
- `ComputerSpec`은 전체 속성 ID를 명시한다. VMID는 가상 Computer에만 적용한다.
- 캐시는 실행마다 새로 만들고 실행 중 갱신하지 않는다. 다른 배치 작업만 실행하면 이 캐시를 조회하지 않는다.
  상세 구조는 [공통 캐시 설계](../../../design/ci/definition-cache.md)를 참조한다.
- 기존 asset처럼 COUNT와 LIMIT·OFFSET으로 조회한다.
  실행 중 원천 행이 추가·삭제되면 페이지 사이에서 누락·중복될 수 있다.
- mapData에서 같은 ComputerSource로 ACTCI·ACTCISPEC DTO를 만든다.
- putData에서 본체를 저장해 ID를 확보한 뒤 스펙의 부모 참조로 전달한다.
- ACTCINUM으로 기존 ACTCIID를 찾고, 신규만 `ACTCISEQ`의 NEXT VALUE를 사용한다.
  스펙도 ACTCINUM·속성·섹션(NULL 포함)으로 기존 ID를 찾고 신규만 `ACTCISPECSEQ`를 사용한다.
- 시퀀스 반환값만 사용하며 중간 ID를 임의로 발급하거나 MAXSEQUENCE·AUTOKEY를 수정하지 않는다.

## 오류·빈 값 처리

- 원천 행 변환·Computer 매핑 실패는 로그를 남기고 다음 건을 처리한다.
- 적용 분류가 없으면 해당 Computer, 자료형 불일치·미지원 단위는 해당 속성을 생략한다.
- 템플릿이 없으면 기본적으로 생략한다. 현재 BIOSRELEASEDATE만 추가 경로를 허용하며,
  전역 ASSETATTRIBUTE가 정확히 한 건 있을 때 CLASSSPECID=NULL, DISPLAYSEQUENCE=180,
  MANDATORY=0, SECTION·연결 속성=NULL로 구성한다. 단위는 속성 정의를 따른다.
- 기존 템플릿이 있으나 ACTCI 적용 설정이나 속성 연결이 잘못된 경우에는 추가 경로로 우회하지 않는다.
  속성 자체의 미등록·전역 동명 정의 중복·조직/사이트 전용 정의만 존재하는 경우도 추가하지 않는다.
- 본체 저장 실패는 해당 Computer의 스펙을 저장하지 않고 다음 Computer를 처리한다.
- 스펙 저장 실패는 다음 스펙과 Computer를 계속 처리한다.
  명시적 트랜잭션·롤백은 없으므로 앞서 저장된 본체·스펙은 남을 수 있다.
- 캐시 조회 실패·중복 분류/스펙 정의는 공통 준비 실패로 CI 실행을 중단한다. 이전 캐시를 재사용하지 않는다.
  준비 후 개별 작업의 실패는 CiIntegrationJob이 기록하고 다음 CI 작업을 진행한다.
- 빈 스펙은 생성하거나 기존 값을 지우지 않는다. 문자열·수치는 임의 절단·반올림하지 않는다.
- LASTSCANDT 누락은 NULL을 전달하므로 대상 필수 제약에 걸리면 본체 저장이 실패한다.
  발견 시각을 실행 시각으로 대체하지 않는다.
- 기존 ACTCI의 분류 변경이나 대상 키 중복은 로그를 남기고 해당 대상을 생략한다.
- 재시도·자동 삭제·분류 변경 정리·관계·승격은 구현 범위에 포함하지 않는다.

## 검증

2026-09-14 기준:

- H2 Db2 모드에서 물리·가상 매핑, 부모·템플릿 참조, 단위·BIOS·코어 값,
  재실행 ID 유지, NULL 섹션 키, 건별 실패 후 계속 처리, 정의 누락·자료형 불일치,
  미지원 단위, 페이지 처리를 검증했다.
- 공통 캐시 공유·다음 실행 갱신·실패 시 이전 캐시 미사용, 추가 속성 NULL 템플릿 적재,
  등록 템플릿으로 전환 시 기존 스펙 ID 유지, 동명 속성·섹션 구분도 테스트한다.
- 기존 원천 SQL은 D42 .68 / .35에서 읽기 전용으로 확인했다.
  이번 COUNT·OFFSET 변경은 실제 D42에서 재실행하지 않았다.
- 이번 리팩터링은 실제 Maximo 업무 행 적재·등록 SQL 실행을 수행하지 않았다.
  특히 템플릿 없는 속성의 실제 화면 표시·승격은 미검증이다.
