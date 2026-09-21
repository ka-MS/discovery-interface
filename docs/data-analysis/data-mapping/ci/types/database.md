# Database — 미구현 검토안

> **현재 실행 명세가 아니다.** 독립 Database의 Query·Mapper·Import는 운영 코드에 없고, `APP.DB.DATABASE`도 실행 분류 목록에 없다.
> 아래 SQL·매핑은 기존 조사·검토 기록이며 `ci` 실행으로 적재되지 않는다. 구현된 DB 연계는 [Database Instance](database-instance.md)다.

> 관측 2026-09-04 · Device42 192.168.2.68 / 192.168.1.35 · Maximo BLUDB

## 1. 범위·분류

DB 본체는 엔진과 무관하게 `APP.DB.DATABASE`로 매핑한다.
CLASSSTRUCTURE에서 해당 CLASSIFICATIONID와 CLASSUSEWITH의 OBJECTNAME='ACTCI'로
분류 ID를 조회한다. 관측된 숫자·문자 ID를 상수로 쓰지 않는다.

Maximo 분류 조회 SQL:

```sql
SELECT S.CLASSSTRUCTUREID
FROM MAXIMO.CLASSSTRUCTURE S
WHERE S.CLASSIFICATIONID = 'APP.DB.DATABASE'
  AND EXISTS (
      SELECT 1
      FROM MAXIMO.CLASSUSEWITH W
      WHERE W.CLASSSTRUCTUREID = S.CLASSSTRUCTUREID
        AND W.OBJECTNAME = 'ACTCI'
  );
```

조회 결과가 정확히 1건일 때 CLASSSTRUCTUREID로 사용한다.

`view_database_v2`가 반환한 DB를 조회하며 databaseinstance_fk가 없는 DB도 보존한다.
Resource와 같은 개체이므로 두 경로에서 중복 본체를 만들지 않는다.
전체 후보 범위는 [ci-targets](../ci-targets.md) 참조.
전체 컬럼의 의미·관측 형식·NULL과 참조값 차이는 [원천 구조](../../../knowledge/device42/database-model.md)에 있다.

## 2. 원천 조회

Device42 원천 조회 SQL이다. 별칭은 d=Database, r=Resource, i=DB Instance다.
미결 속성의 조사용 값도 반환하며, 모든 반환 컬럼이 적재 대상으로 확정된 것은 아니다.

```sql
SELECT
    D.database_pk AS source_pk,
    D.database_name AS source_name,
    D.database_id,
    D.creation_date,
    D."collate" AS collation,
    D.compatibility_level,
    D.recovery_model,
    D.allocated_size,
    R.resource_pk,
    R.identifier AS resource_identifier,
    R.notes AS source_description,
    R.vendor_resource_type,
    R.vendor_resource_subtype,
    R.last_discovered AS resource_last_discovered,
    R.last_changed AS resource_last_changed,
    R.root_resource_fk,
    D.databaseinstance_fk AS instance_source_pk,
    D.instance_id,
    I.database_type AS instance_engine,
    I.dbinstance_name AS instance_name
FROM view_database_v2 D
LEFT JOIN view_resource_v2 R
    ON R.resource_pk = D.database_pk
LEFT JOIN view_databaseinstance_v2 I
    ON I.databaseinstance_pk = D.databaseinstance_fk
ORDER BY D.database_pk;
```

| Source | 조인 | 역할 |
| --- | --- | --- |
| `view_database_v2 d` | 기준 행 | DB 이름·DB 고유 속성·Instance FK |
| `view_resource_v2 r` | LEFT JOIN `r.resource_pk=d.database_pk` | 동일 본체의 식별자·발견 시각 |
| `view_databaseinstance_v2 i` | LEFT JOIN `i.databaseinstance_pk=d.databaseinstance_fk` | 연결된 Instance 정보 |

논리 원천 키는 `(resource, source_pk)`다. source_pk는 d.database_pk이며,
Resource 참조 시 r.resource_pk와 일치해야 한다. ACTCINUM·ACTCIID 생성 규칙과는 별개다.
조인 실패를 INNER JOIN으로 숨기거나 이름으로 대체 조인하지 않는다.

| 검증 | .68 | .35 |
| --- | ---: | ---: |
| 조회 DB / 고유 source_pk | 10 / 10 | 0 / 0 |
| 동일 Resource 연결 | 10 | 0 |
| Instance 연결 | 9 | 0 |
| resource_last_discovered 보유 | 0 | 0 |

### 원천 컬럼별 사용

전체 10컬럼과 SQL에 포함된 보강 값을 빠짐없이 구분한다. 미결 속성도 원천 값은 조회한다.

| Source → SQL 결과 | 용도 / Target | 변환·조건 |
| --- | --- | --- |
| d.database_pk → source_pk | DB 본체 식별·관계 도착점 탐색 | 논리 키의 숫자 부분. ACTCIID/ACTCINUM 직접 대입 아님 |
| d.database_name → source_name | ACTCI.ACTCINAME; 이름 속성 | 문자열 그대로. 본체 192자·속성 254자 한도 초과 시 절단하지 않음 |
| d.database_id → database_id | 속성 미결 | DB 내부 ID. DATABASE_ASSETID나 ACTCIID로 대체하지 않음. ISSUE-11 |
| d.creation_date → creation_date | 속성 미결 | 시각·오프셋 보존. 발견 시각으로 대체하지 않음. ISSUE-11 |
| d.collate → collation | 속성 미결 | 정렬 규칙 문자열 그대로. ISSUE-11 |
| d.compatibility_level → compatibility_level | 속성 미결 | 제품 버전으로 변환하지 않음. ISSUE-11 |
| d.recovery_model → recovery_model | 속성 미결 | 복구 모드 문자열 그대로. ISSUE-11 |
| d.allocated_size → allocated_size | 속성 미결 | 단위 미확인; GB 등으로 환산하지 않음. ISSUE-11 |
| d.databaseinstance_fk → instance_source_pk | Instance→DB 관계 | 5절. NULL이면 현재 FK 기반 관계 SQL에서는 제외; 본체 유지 |
| d.instance_id → instance_id | 참조 차이 진단 | FK 대체 사용 미결. ISSUE-11 |
| r.resource_pk → resource_pk | 동일 Resource 연결 확인 | source_pk와 일치; 별도 ACTCI 생성 안 함 |
| r.identifier → resource_identifier | GUID 후보 | 문자열을 조회하되 식별자 정책은 ISSUE-11 |
| r.notes → source_description | ACTCI.DESCRIPTION | 문자열 그대로; 빈 문자열과 NULL 구분 유지. 1024자 초과 시 절단하지 않음 |
| r.last_discovered → resource_last_discovered | ACTCI.LASTSCANDT 원천 | NULL 처리 미결. ISSUE-11 |
| r.last_changed → resource_last_changed | 원천 변경 시각 확인 | LASTSCANDT·CHANGEDATE 대체 규칙 없음 |
| r.vendor_resource_type / vendor_resource_subtype | 원천 유형 확인 | 분류는 1절. 엔진·유형 값으로 임의 필터하지 않음 |
| r.root_resource_fk → root_resource_fk | 참조 차이 진단 | Instance 관계 보강 여부 미결. ISSUE-11 |
| i.database_type / dbinstance_name → instance_engine / instance_name | 연결된 Instance 정보 | DB 이름·DB 분류의 대체값으로 사용하지 않음 |

## 3. 본체 매핑

공통 컬럼·타입·필수값은 [ACTCI](../actci.md)에 둔다. 아래는 유형별 차이만 정의한다.

| Target 컬럼 | 한글명 | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- |
| ACTCINAME | 실제 구성 품목 이름 | 직접 | `d.database_name` | 조회 결과 source_name |
| DESCRIPTION | 설명 | 직접 | `r.notes` | 조회 결과 source_description; NULL 유지 |
| CLASSSTRUCTUREID | 분류 | 변환 | Maximo 분류 조회 | 1절의 일반 DB 분류 ID |
| GUID | 발견 ID | 미결 | `r.identifier` | 원천 식별자 보유와 Target GUID 정책은 별개. ISSUE-11 |
| LASTSCANDT | 최종 스캔 날짜 | 미결 | `r.last_discovered` | 전건 NULL. 다른 시각으로 대체하지 않음. ISSUE-11 |

## 4. 속성 매핑

분류: `APP.DB.DATABASE`. 부모·템플릿 참조는 [ACTCISPEC](../actcispec.md)을 따른다.

| ASSETATTRID | 한글 의미 | 값 컬럼 | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- |
| SDEPLOYABLECOMPONENT_NAME | DB 이름 | ALNVALUE | 직접 | `d.database_name` | SECTION=NULL인 템플릿 사용. 속성 ID 연결과 ACTCI용 적용 설정 보완 후 적재 |

Maximo 속성 템플릿 조회 SQL이다. 1절에서 선택한 분류와 일치하는 1건을 사용한다.
속성 ID 연결과 ACTCI용 적용 설정을 모두 요구하므로, 관측된 설정 누락 상태에서는 반환되지 않는다.

```sql
SELECT
    C.CLASSSTRUCTUREID,
    C.CLASSSPECID,
    C.ASSETATTRID,
    A.DATATYPE,
    C.SECTION,
    C.MEASUREUNITID,
    C.LINKEDTOATTRIBUTE,
    C.LINKEDTOSECTION,
    U.MANDATORY,
    U.SEQUENCE AS DISPLAYSEQUENCE
FROM MAXIMO.CLASSSTRUCTURE S
JOIN MAXIMO.CLASSSPEC C
    ON C.CLASSSTRUCTUREID = S.CLASSSTRUCTUREID
JOIN MAXIMO.ASSETATTRIBUTE A
    ON A.ASSETATTRIBUTEID = C.ASSETATTRIBUTEID
   AND A.ASSETATTRID = C.ASSETATTRID
JOIN MAXIMO.CLASSSPECUSEWITH U
    ON U.CLASSSPECID = C.CLASSSPECID
   AND U.OBJECTNAME = 'ACTCI'
WHERE S.CLASSIFICATIONID = 'APP.DB.DATABASE'
  AND C.ASSETATTRID = 'SDEPLOYABLECOMPONENT_NAME'
  AND C.SECTION IS NULL
  AND A.DATATYPE = 'ALN'
  AND EXISTS (
      SELECT 1
      FROM MAXIMO.CLASSUSEWITH W
      WHERE W.CLASSSTRUCTUREID = S.CLASSSTRUCTUREID
        AND W.OBJECTNAME = 'ACTCI'
  );
```

일반 분류의 설정 누락은 [CI 분류 모델](../../../knowledge/maximo/ci-classification.md)에 있다.
설정 보완 전에는 공통 템플릿 조회 조건을 충족하지 않는다.
DB ID·생성일·정렬 규칙·복구 모드·할당 크기를 SQL Server 전용 속성에 넣지 않는다.
이 값의 일반 분류 속성 대응은 ISSUE-11에 둔다.

## 5. 관계

Instance→DB 관계의 원천 조회·방향은 [DB Instance](database-instance.md#5-관계)에만 정의한다.
databaseinstance_fk가 NULL인 DB는 현재 FK 기반 관계 조회에서 제외된다.
instance_id·root_resource_fk로 관계를 보강할지는 ISSUE-11이며, 본체는 제외하지 않는다.

## 6. 미결

[ISSUE-11](../../../open-issues.md#issue-11-actual-ci-분류속성관계와-식별자-매핑) — DB 속성 적용 설정·추가 속성, 관계 규칙, 식별자·필수값 정책.
전체 원천 컬럼의 사용처와 미결 사유를 작성했다. 적재 실행·UI 검증은 하지 않았다.
