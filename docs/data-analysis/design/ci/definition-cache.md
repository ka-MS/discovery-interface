# CI 공통 정의 캐시

> 결정: 2026-09-14 · 사용 분류 목록에 따른 실행 단위 캐시와 명시적 추가 속성 경로.
> Computer 필드·조회 SQL 정본: [Computer 매핑](../../data-mapping/ci/types/computer.md).

## 구성

| 코드 | 책임 |
| --- | --- |
| CiClassification | 사용할 분류명 목록. SQL 조건을 별도 수정하지 않음 |
| ComputerSpec | 전체 ASSETATTRID·적용 대상·명시적 추가 속성 설정 |
| CiDefinitionLoader | 선택한 ACTCI 분류·스펙과 전체 ASSETATTRIBUTE를 조회 |
| CiDefinitionCache | 실행 동안 공유하는 읽기 전용 스냅샷. DB 접근 없음 |
| CiIntegrationJob | 실행마다 캐시 한 번 생성, 같은 객체를 모든 작업에 전달 |
| ComputerCiIntegrate | getData → mapData → putData. 정의는 전달받은 캐시 참조 |

분류는 분류명, 스펙은 (CLASSSTRUCTUREID, ASSETATTRID, SECTION), 속성은 숫자
ASSETATTRIBUTEID로 조회한다. NULL 섹션을 빈 문자열로 바꾸지 않는다.
다른 접두어의 속성도 같은 키 규칙을 사용한다.

## 조직·사이트 범위

캐시는 ORGID·SITEID가 NULL인 전역 템플릿만 담는다. CLASSSPEC과 CLASSSPECUSEWITH의
고유 인덱스는 두 컬럼을 포함하므로, 조회에서 제외해야 스펙 키 세 컬럼이 실제로 유일해진다.
제외하지 않으면 조직 전용 행이 정상 데이터인데도 중복으로 판정되어 CI 실행 전체가 중단된다.
관측값은 [CI 분류 모델](../../knowledge/maximo/ci-classification.md)의 조직·사이트 범위 절에 있다.
조직별 적재 요구가 생기면 스펙 키에 두 컬럼을 추가하고 조회 기준을 함께 정한다.

## 추가 속성

1. 해당 분류·속성·섹션의 정상 템플릿이 있으면 그 설정을 사용한다.
2. 템플릿 자체가 없고 수집 enum에서 추가 속성으로 허용한 경우에만 전역 속성 정의를 찾는다.
3. 전역 ASSETATTRIBUTE가 정확히 한 건이면 자료형·단위를 가져오고 템플릿 ID·연결 속성은 NULL로 둔다.
   표시 순서·필수 여부는 명시한 추가 설정을 사용한다.
4. 미등록·동명 전역 속성 중복·기존 템플릿의 적용 설정 오류는 해당 속성을 생략한다.

현재 허용 항목은 Computer BIOS 출시일뿐이다. 실제 기준정보 자동 생성은 하지 않는다.
다른 분류의 템플릿을 가져와 연결하지 않는다. UI·승격 확인은 ISSUE-11의 후속 검증이다.

## 실행과 확장

- 다음 유형 구현 시 CiClassification에 분류명을 추가하고 유형별 속성 enum·수집 작업을 만든다.
- 캐시 로더의 SQL이나 Computer 코드를 수정해서 새 분류를 열거하지 않는다.
- 조회 결과는 실행 중 갱신하지 않으며, 다음 실행은 새 캐시를 만든다.
- 공통 준비 실패 시 이전 캐시를 사용하지 않는다. 준비 이후 개별 작업 실패는 다음 작업으로 이어간다.
- 재시도·트랜잭션·TTL·상시 갱신 기능은 추가하지 않는다.
