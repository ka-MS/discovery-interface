# ACTCI

실제 CI 테이블

> Target: MAXIMO.ACTCI · 공통 규약 / 원천 필드는 유형 문서
> 관측 2026-09-04 · Maximo BLUDB
> 구조 분석: [CI 모델](../../knowledge/maximo/ci-model.md)

## 1. 관계

- 물리 PK: `ACTCIID`; 고유 인덱스: `ACTCINUM`.
- 분류: `CLASSSTRUCTUREID` → `CLASSSTRUCTURE.CLASSSTRUCTUREID`.
- 자식: `ACTCISPEC.REFOBJECTID=ACTCI.ACTCIID`.
- CI 연결: `CI.ACTCINUM=ACTCI.ACTCINUM`; `CI.CINUM`과는 별도다.

## 2. 테이블 매핑

유형별 원천은 [Device](types/device.md), [DB](types/database.md), [DB Instance](types/database-instance.md)에 둔다.
후보 범위와 중복 근거는 [ci-targets.md](ci-targets.md) 참조.
본체를 먼저 식별·저장하고 저장된 ACTCIID·ACTCINUM을 속성과 관계에서 참조한다.
원천 SQL의 반환 컬럼과 변환식은 각 유형 문서에 작성한다.

## 3. 조회 조건

DB는 `view_database_v2`, Instance는 `view_databaseinstance_v2`의 전 행을 조회한다.
엔진 필터는 없으며 보강 원천은 LEFT JOIN한다. 실제 SQL은 유형 문서 2절에 있다.
Computer의 물리·VM 수집 조건은 유형 문서 2절에 있다. 그 밖의 CI 유형 범위는 [ISSUE-8](../../open-issues.md#issue-8-actual-ci-대상-범위)이다.

## 4. 컬럼 매핑

영속 14개 컬럼이다. 앞 4열은 Maximo 메타데이터다. 유형별 변환 결과를 받는
컬럼과 아직 결정되지 않은 공통 적재 정책을 구분한다.

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| ACTCIID | 고유 ID | BIGINT(19) | N | 미결 | Maximo 채번 | 물리 PK; ACTCISEQ 예약·발번 정책 ISSUE-11. 원천 PK 직접 대입 안 함 |
| ACTCINAME | 실제 구성 품목 이름 | UPPER(192) | Y | 직접 | 유형별 원천 이름: Computer는 `view_device_v2.name` | 임의 대문자 변환·절단 없음; DB·Instance 원천은 각 유형 문서 |
| ACTCINUM | 실제 구성 품목 번호 | UPPER(150) | N | 변환 | `D42:<원천 개체 종류>:<원천 PK>` | 문자열 sourceId 저장으로 합의. Computer는 DEVICE; 원천 개체 종류는 유형별 정의 |
| CCIDISGUID | 통합 ID | ALN(192) | Y | 미결 | 전용 원천 없음 | GUID와의 구분·생성 정책 ISSUE-11 |
| CHANGEBY | 변경자 | UPPER(100) | Y | 원천없음 | D42 해당 본체의 변경자 미제공 | ETL 계정 적용 여부 ISSUE-11 |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | Y | 미결 | 원천 후보: `view_resource_v2.last_changed` | 원천 변경 시각 / ETL 시각 적용 정책 ISSUE-11 |
| CLASSSTRUCTUREID | 분류 | UPPER(25) | Y | 변환 | `MAXIMO.CLASSSTRUCTURE.CLASSSTRUCTUREID` | 유형별 분류 조회 SQL; ACTCI 적용 설정 확인 |
| DESCRIPTION | 설명 | ALN(1024) | Y | 직접 | 유형별 원천 설명: Computer는 `view_device_v2.notes`, DB·Instance는 `view_resource_v2.notes` | NULL 유지, 임의 절단 없음 |
| EXTENDEDINSTANCES | ExtendedInstances | CLOB(999999) | Y | 원천없음 | 확인된 직접 대응 없음 | Resource.details를 임의 직렬화해 넣지 않음; 필요성 ISSUE-11 |
| GUID | 발견 ID | ALN(192) | Y | 미결 | `view_resource_v2.identifier` | DB·Instance 모두 원천 보유; 적용·동일성 정책 ISSUE-11 |
| HASLD | 상세 설명 있음 | YORN(1) | N | 미결 | Maximo 메타데이터 기본값 0 | 물리 DEFAULT 없음; JDBC 기본값 적용 정책 ISSUE-11 |
| LANGCODE | 언어 코드 | UPPER(4) | N | 원천없음 | D42 언어 코드 미제공 | 필수값 생성 정책 ISSUE-11 |
| LASTSCANDT | 최종 스캔 날짜 | DATETIME(10) | N | 미결 | Computer: `view_device_v2.last_discovered`; DB Instance: `view_resource_v2.last_changed` | REQUIRED=1이라 값이 없으면 적재되지 않는다. Instance는 last_discovered가 전건 NULL이어서 last_changed를 쓴다. 나머지 유형의 원천 대응은 유형별 정의; 시간대 처리 ISSUE-11 |
| PLUSPCUSTOMER | 기본 고객 | UPPER(12) | Y | 원천없음 | 확인된 Maximo 고객 코드 대응 없음 | Component의 customer_fk를 직접 대입하지 않음; ISSUE-11 |

## 5. SQL

실제 원천 SQL과 분류 조회 SQL은 각 유형 문서 본문에 있다.
적재 식별자·필수값 정책이 미결이므로 INSERT/MERGE SQL은 아직 확정하지 않았다.

## 6. 미결

[ISSUE-11](../../open-issues.md#issue-11-actual-ci-분류속성관계와-식별자-매핑) — 분류·속성·관계 코드와 식별자·필수값 적재 규칙 미정.
