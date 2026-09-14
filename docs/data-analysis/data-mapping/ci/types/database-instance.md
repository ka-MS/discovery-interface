# Database Instance

> 관측 2026-09-04 · Device42 192.168.2.68 / 192.168.1.35 · Maximo BLUDB

## 1. 범위·분류

DB Instance는 엔진과 무관하게 `APP.DB.DATABASESERVER`로 매핑한다.
CLASSSTRUCTURE에서 해당 CLASSIFICATIONID와 CLASSUSEWITH의 OBJECTNAME='ACTCI'로
분류 ID를 조회한다. 물리 장치가 아니라 DB 서버 소프트웨어 Instance의 본체다.

Maximo 분류 조회 SQL:

```sql
SELECT S.CLASSSTRUCTUREID
FROM MAXIMO.CLASSSTRUCTURE S
WHERE S.CLASSIFICATIONID = 'APP.DB.DATABASESERVER'
  AND EXISTS (
      SELECT 1
      FROM MAXIMO.CLASSUSEWITH W
      WHERE W.CLASSSTRUCTUREID = S.CLASSSTRUCTUREID
        AND W.OBJECTNAME = 'ACTCI'
  );
```

조회 결과가 정확히 1건일 때 CLASSSTRUCTUREID로 사용한다.

`view_databaseinstance_v2`가 기준이며 연결된 Application Component는 보강 원천이다.
Application Component 자체의 CI 적재 여부는 [ISSUE-8](../../../open-issues.md#issue-8-actual-ci-대상-범위)에 둔다.
동일 PK의 Resource는 같은 Instance의 보강 원천이며 별도 본체를 생성하지 않는다.
전체 컬럼의 의미·관측 형식·NULL은 [원천 구조](../../../knowledge/device42/database-model.md)에 있다.

## 2. 원천 조회

Device42 원천 조회 SQL이다. 별칭은 i=DB Instance, r=Resource, a=Application Component, h=Device다.
미결 속성과 장치 연결의 조사용 값도 반환하며, 모든 반환 컬럼이 적재 대상으로 확정된 것은 아니다.

```sql
SELECT
    I.databaseinstance_pk AS source_pk,
    I.dbinstance_name AS source_name,
    I.database_type AS engine,
    I.db_type_id,
    I.host_name,
    I.database_count,
    I.connection_count,
    I.is_default_instance,
    R.resource_pk,
    R.identifier AS resource_identifier,
    R.notes AS source_description,
    R.last_discovered AS resource_last_discovered,
    R.last_changed AS resource_last_changed,
    CAST(R.details AS JSONB)->>'version' AS version_text,
    I.appcomp_fk,
    A.name AS appcomp_name,
    A.device_fk AS host_source_pk,
    A.last_changed AS appcomp_last_changed,
    H.last_discovered AS host_last_discovered,
    H.device_pk AS matched_host_pk
FROM view_databaseinstance_v2 I
LEFT JOIN view_resource_v2 R
    ON R.resource_pk = I.databaseinstance_pk
LEFT JOIN view_appcomp_v1 A
    ON A.appcomp_pk = I.appcomp_fk
LEFT JOIN view_device_v2 H
    ON H.device_pk = A.device_fk
ORDER BY I.databaseinstance_pk;
```

| Source | 조인 | 역할 |
| --- | --- | --- |
| `view_databaseinstance_v2 i` | 기준 행 | Instance 이름·엔진·DB 수·연결 수 |
| `view_resource_v2 r` | LEFT JOIN `r.resource_pk=i.databaseinstance_pk` | 동일 본체의 식별자·메모·발견 시각·버전 문자열 |
| `view_appcomp_v1 a` | LEFT JOIN `a.appcomp_pk=i.appcomp_fk` | 장치 연결·Component 변경 시각 |
| `view_device_v2 h` | LEFT JOIN `h.device_pk=a.device_fk` | 실제 장치 연결 검증 |

논리 원천 키는 `(database-instance, source_pk)`이며 source_pk는 i.databaseinstance_pk다.
ACTCINUM·ACTCIID 생성 규칙과는 별개다. Component·Device가 없어도 Instance 행은 보존한다.

| 검증 | .68 | .35 |
| --- | ---: | ---: |
| 조회 Instance / 고유 source_pk | 1 / 1 | 0 / 0 |
| 동일 Resource 연결 | 1 | 0 |
| resource_last_discovered 보유 | 0 | 0 |
| Application Component 연결 | 1 | 0 |
| 장치 연결 | 0 | 0 |
| Instance에 연결된 DB | 9 | 0 |

### 원천 컬럼별 사용

전체 9컬럼과 SQL에 포함된 보강 값을 빠짐없이 구분한다. 미결 속성도 원천 값은 조회한다.

| Source → SQL 결과 | 용도 / Target | 변환·조건 |
| --- | --- | --- |
| i.databaseinstance_pk → source_pk | Instance 식별·관계 출발점 탐색 | 논리 키의 숫자 부분. ACTCIID/ACTCINUM 직접 대입 아님 |
| i.dbinstance_name → source_name | ACTCI.ACTCINAME; APPSERVER_NAME | 문자열 그대로. 본체 192자·속성 254자 초과 시 절단하지 않음 |
| i.database_type → engine | APPSERVER_PRODUCTNAME | 엔진 표기 그대로. 일반 DB Server 분류는 유지 |
| i.db_type_id → db_type_id | 엔진 구분값 확인 | 이름 원천은 database_type. 관측 숫자를 분류 ID로 사용하지 않음 |
| i.host_name → host_name | 호스트 연결 조사 | 장치 이름 대체 조인에 사용하지 않음 |
| i.appcomp_fk → appcomp_fk | Component 경유 장치 탐색 | a.appcomp_pk와 조인. Instance 자체 ID가 아님 |
| i.database_count → database_count | 속성 미결·관계 건수 검증 | Target 속성 미선정. ISSUE-11 |
| i.connection_count → connection_count | 속성 미결 | 집계 시점·범위와 Target 속성 미확정. ISSUE-11 |
| i.is_default_instance → is_default_instance | 속성 미결 | Boolean 원천 보존. APPSERVER_ISPLACEHOLDER와 의미가 다름. ISSUE-11 |
| r.resource_pk → resource_pk | 동일 Resource 연결 확인 | source_pk와 일치; 별도 ACTCI 생성 안 함 |
| r.identifier → resource_identifier | GUID 후보 | 식별자 정책은 ISSUE-11 |
| r.notes → source_description | ACTCI.DESCRIPTION | 문자열 그대로; 빈 문자열과 NULL 구분 유지. 1024자 초과 시 절단하지 않음 |
| r.last_discovered → resource_last_discovered | ACTCI.LASTSCANDT 원천 | NULL 처리 미결. ISSUE-11 |
| r.last_changed → resource_last_changed | 원천 변경 시각 확인 | LASTSCANDT·CHANGEDATE 대체 규칙 없음 |
| r.details.version → version_text | APPSERVER_VERSIONSTRING | JSON 키가 없으면 NULL. 원문 보존; 254자 초과 시 절단하지 않음 |
| a.name → appcomp_name | Component 연결 확인 | Instance 이름을 덮어쓰지 않음 |
| a.device_fk → host_source_pk | Instance→장치 관계 후보 | 실제 장치 h.device_pk 연결 필요; 대상 유형·관계 적용은 미결 |
| a.last_changed / h.last_discovered | 연결 대상 시각 확인 | Instance 발견 시각으로 사용하지 않음 |
| h.device_pk → matched_host_pk | 장치 연결 성공 확인 | 원천 FK 보유와 실제 조인 성공을 구분 |

Resource JSON의 나머지 키는 [원천 구조](../../../knowledge/device42/database-model.md)에 분석했다.
정규 컬럼과 중복인 database_type·default_instance는 중복 매핑하지 않는다.
주소·CPU·메모리·시작 시각·메모리 상태의 Target 대응은 ISSUE-11이다.

## 3. 본체 매핑

공통 컬럼·타입·필수값은 [ACTCI](../actci.md)를 따른다.

| Target 컬럼 | 한글명 | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- |
| ACTCINAME | 실제 구성 품목 이름 | 직접 | `i.dbinstance_name` | 조회 결과 source_name; Component 이름으로 대체하지 않음 |
| DESCRIPTION | 설명 | 직접 | `r.notes` | 조회 결과 source_description; NULL 유지 |
| CLASSSTRUCTUREID | 분류 | 변환 | Maximo 분류 조회 | 1절의 일반 DB Server 분류 ID |
| GUID | 발견 ID | 미결 | `r.identifier` | 원천 식별 문자열은 존재; Target 식별자 정책은 ISSUE-11 |
| LASTSCANDT | 최종 스캔 날짜 | 미결 | `r.last_discovered` | 동일 Resource도 전건 NULL. 다른 시각으로 대체하지 않음. ISSUE-11 |

## 4. 속성 매핑

분류: `APP.DB.DATABASESERVER`. 부모·템플릿 참조는 [ACTCISPEC](../actcispec.md)을 따른다.

| ASSETATTRID | 한글 의미 | 값 컬럼 | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- |
| APPSERVER_NAME | Instance 이름 | ALNVALUE | 직접 | `i.dbinstance_name` | SECTION=NULL |
| APPSERVER_PRODUCTNAME | DB 제품명 | ALNVALUE | 직접 | `i.database_type` | 엔진 표기를 보존. 엔진별 분류로 분기하지 않음 |
| APPSERVER_VERSIONSTRING | DB 버전 문자열 | ALNVALUE | 직접 | `CAST(r.details AS JSONB)->>'version'` | 원문 그대로; 키 미보유는 NULL. 표본 1건·238자, 제품 버전 번호만 추출하지 않음 |

Maximo 속성 템플릿 조회 SQL이다. 1절에서 선택한 분류와 일치하며,
각 ASSETATTRID·SECTION에 정확히 1건인 결과를 사용한다.

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
WHERE S.CLASSIFICATIONID = 'APP.DB.DATABASESERVER'
  AND C.ASSETATTRID IN ('APPSERVER_NAME', 'APPSERVER_PRODUCTNAME', 'APPSERVER_VERSIONSTRING')
  AND C.SECTION IS NULL
  AND A.DATATYPE = 'ALN'
  AND EXISTS (
      SELECT 1
      FROM MAXIMO.CLASSUSEWITH W
      WHERE W.CLASSSTRUCTUREID = S.CLASSSTRUCTUREID
        AND W.OBJECTNAME = 'ACTCI'
  )
ORDER BY C.ASSETATTRID;
```

세 속성의 ASSETATTRIBUTE 연결과 ACTCI용 적용 설정은 존재한다.
표시 순서·필수 여부는 공통 규약에 따라 적용 설정에서 조회한다.
DB 수·연결 수·기본 Instance 여부의 속성 대응은 ISSUE-11에 둔다.

## 5. 관계

이 문서가 Instance에서 출발하는 관계 정의의 정본이다.

| 관계 | 원천 연결 | 저장할 출발 / 도착 본체 | 상태 |
| --- | --- | --- | --- |
| Instance → DB | `d.databaseinstance_fk=i.databaseinstance_pk` | (database-instance, i.databaseinstance_pk) / (resource, d.database_pk) | 연결 원천 확인; 관계 코드·규칙 미결 |
| Instance → 장치 | `i.appcomp_fk=a.appcomp_pk AND a.device_fk=h.device_pk` | Instance / 연결된 Device | 표본 0건; 장치 유형 매핑과 관계 규칙 적용 미결 |

Instance→DB 원천 관계 조회 SQL:

```sql
SELECT
    I.databaseinstance_pk AS instance_source_pk,
    D.database_pk AS database_source_pk,
    I.database_type AS engine
FROM view_databaseinstance_v2 I
JOIN view_database_v2 D
    ON D.databaseinstance_fk = I.databaseinstance_pk
ORDER BY I.databaseinstance_pk, D.database_pk;
```

DB의 FK 방향과 반대로 Instance를 출발 본체로 삼으며, 양 끝의 저장된 ACTCINUM을 참조한다.
원천 쌍은 .68에서 9건이고 .35에서 0건이다.
instance_id·root_resource_fk 기준은 10쌍이다. 현재 SQL은 명시된 FK 기준만 사용하며,
불일치 1건의 관계 보강 여부는 ISSUE-11이다.

Maximo 분류쌍 관계 규칙 조회 SQL이다. 관계 코드가 미결이므로 결과에서 임의로 선택하지 않는다.

```sql
SELECT
    S.CLASSIFICATIONID AS SOURCE_CLASS,
    T.CLASSIFICATIONID AS TARGET_CLASS,
    R.RELATIONNUM,
    R.CARDINALITY,
    R.CONTAINMENT,
    R.REVRELATIONSHIP,
    R.SWAPPED
FROM MAXIMO.RELATIONRULES R
JOIN MAXIMO.RELATION L
    ON L.RELATIONNUM = R.RELATIONNUM
JOIN MAXIMO.CLASSSTRUCTURE S
    ON S.CLASSSTRUCTUREID = R.SOURCECLASS
JOIN MAXIMO.CLASSSTRUCTURE T
    ON T.CLASSSTRUCTUREID = R.TARGETCLASS
WHERE S.CLASSIFICATIONID = 'APP.DB.DATABASESERVER'
  AND T.CLASSIFICATIONID = 'APP.DB.DATABASE';
```

일반 DB Server→일반 DB 분류쌍의 RELATIONRULES는 0건이다.
RELATION.CONTAINS 코드가 존재해도 분류쌍 규칙이 있다는 뜻은 아니다.
관계 코드를 확정하기 전에는 정상 적재 가능으로 표시하지 않는다.

## 6. 미결

[ISSUE-11](../../../open-issues.md#issue-11-actual-ci-분류속성관계와-식별자-매핑) — 추가 속성·관계 규칙·식별자·필수값 정책.
전체 원천 컬럼의 사용처와 미결 사유를 작성했다. 적재 실행·UI 검증은 하지 않았다.
