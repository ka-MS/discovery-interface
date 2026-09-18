# Database Instance

> 구현: [DatabaseInstanceCiImport](../../../../../src/main/java/com/itmsg/device42/pipeline/d42maximo/ci/databaseinstance/DatabaseInstanceCiImport.java) · [DatabaseInstanceCiQuery](../../../../../src/main/java/com/itmsg/device42/source/device42/ci/databaseinstance/DatabaseInstanceCiQuery.java) · [DatabaseInstanceCiMapper](../../../../../src/main/java/com/itmsg/device42/pipeline/d42maximo/ci/databaseinstance/DatabaseInstanceCiMapper.java) · [ActCiWriter](../../../../../src/main/java/com/itmsg/device42/target/maximo/ci/ActCiWriter.java)

> 관측 2026-09-16 · Device42 192.168.2.68 / 192.168.1.35 · Maximo BLUDB

> SQL의 LIMIT/OFFSET은 예시 페이지 값이다. 본체·관계 조회는 Source, 타겟 식별자·값 생성은 Pipeline Mapper가 소유한다.

## 1. 범위·분류

DB Instance는 `database_type`에 따라 엔진별 분류로 라우팅하고, 전용 분류가 없는 엔진은
범용 분류로 보낸다. 물리 장치가 아니라 DB 서버 소프트웨어 Instance의 본체다.
선택 이유와 비용은 [Database CI 수집 설계](../../../design/ci/databaseinstance.md) 3절에 있다.

| `i.database_type` | Actual CI 분류 | 승격 대상 CI 분류 |
| --- | --- | --- |
| `Microsoft SQL` | `APP.DB.MSSQL.SQLSERVER` | `CI.SQLSERVER` |
| `DB2` | `APP.DB.DB2.DB2INSTANCE` | `CI.DB2INSTANCE` |
| `Oracle Database` | `APP.DB.ORACLE.ORACLEINSTANCE` | `CI.ORACLEINSTANCE` |
| 그 밖의 값·NULL | `APP.DB.DATABASESERVER` | `CI.DATABASESERVER` |

`database_type`은 표기 그대로 비교한다. `db_type_id`의 관측 숫자를 분류 판정에 쓰지 않는다.
재수집에서 값이 바뀔 수 있는 원천 코드이며, 이름 원천은 `database_type`이다.
엔진별 분류가 없는 경우에만 `APP.DB.DATABASESERVER`로 보낸다. 기존 단일 분류 매핑에서
엔진 세 갈래가 빠져나간 형태이며, 이미 적재된 행이 있으면 분류만 갱신된다.
범용 분류로 `APP.DB.GENERICDATABASESERVER`를 쓰지 않는 이유는 4절에 있다.
승격 CI 분류는 매핑 대상이 아니라 MAS UI 승격 범위 등록 대상이다.

Maximo 분류 조회 SQL이다. 네 분류가 모두 ACTCI 적용이며 각 CLASSIFICATIONID에 1건이다.

```sql
SELECT S.CLASSIFICATIONID, S.CLASSSTRUCTUREID
FROM MAXIMO.CLASSSTRUCTURE S
WHERE S.CLASSIFICATIONID IN (
        'APP.DB.MSSQL.SQLSERVER',
        'APP.DB.DB2.DB2INSTANCE',
        'APP.DB.ORACLE.ORACLEINSTANCE',
        'APP.DB.DATABASESERVER'
      )
  AND EXISTS (
      SELECT 1
      FROM MAXIMO.CLASSUSEWITH W
      WHERE W.CLASSSTRUCTUREID = S.CLASSSTRUCTUREID
        AND W.OBJECTNAME = 'ACTCI'
  )
ORDER BY S.CLASSIFICATIONID;
```

`view_databaseinstance_v2`가 기준이며 연결된 Application Component는 보강 원천이다.
Application Component 자체의 CI 적재 여부는 [ISSUE-8](../../../open-issues.md#issue-8-actual-ci-대상-범위)에 둔다.
동일 PK의 Resource는 같은 Instance의 보강 원천이며 별도 본체를 생성하지 않는다.
전체 컬럼의 의미·관측 형식·NULL은 [원천 구조](../../../knowledge/device42/database-model.md)에 있다.

## 2. 원천 조회

현재 배치의 본체 조회 SQL이다. 아래 뒤쪽의 넓은 조사용 조회와 구분한다.

```sql
SELECT i.databaseinstance_pk,
    NULLIF(TRIM(i.dbinstance_name), '') AS dbinstance_name,
    NULLIF(TRIM(i.database_type), '') AS database_type,
    NULLIF(TRIM(r.identifier), '') AS resource_identifier,
    NULLIF(TRIM(CAST(r.details AS JSONB)->>'version'), '') AS version_text,
    r.notes AS source_description,
    r.last_changed,
    NULLIF(TRIM(CAST(a.json AS JSONB)->'products'->0->>'install_path'), '') AS install_path
FROM view_databaseinstance_v2 i
LEFT JOIN view_resource_v2 r ON r.resource_pk = i.databaseinstance_pk
LEFT JOIN view_appcomp_v1 a ON a.appcomp_pk = i.appcomp_fk
ORDER BY i.databaseinstance_pk
LIMIT 1000 OFFSET 0
```


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
    CAST(A.json AS JSONB)->'products'->0->>'install_path' AS install_path,
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
| 조회 Instance / 고유 source_pk | 1 / 1 | 3 / 3 |
| 동일 Resource 연결 | 1 | 3 |
| resource_last_discovered 보유 | 0 | 0 |
| resource_last_changed 보유 | 1 | 3 |
| source_description(notes) 보유 | 0 | 0 |
| version_text 보유 | 1 | 3 |
| host_name 보유 | 0 | 3 |
| Application Component 연결 | 1 | 3 |
| Component products.install_path 보유 | 0 | 1 |
| 장치 연결 | 0 | 3 |
| Instance에 연결된 DB | 9 | 37 |

`notes`는 두 서버 모두 빈 문자열이며 NULL은 아니다. `.68`의 Component는 products가 없다.

### 원천 컬럼별 사용

전체 9컬럼과 SQL에 포함된 보강 값을 빠짐없이 구분한다. 미결 속성도 원천 값은 조회한다.

| Source → SQL 결과 | 용도 / Target | 변환·조건 |
| --- | --- | --- |
| i.databaseinstance_pk → source_pk | Instance 식별·관계 출발점 탐색 | 논리 키의 숫자 부분. ACTCIID/ACTCINUM 직접 대입 아님 |
| i.dbinstance_name → source_name | ACTCI.ACTCINAME; APPSERVER_NAME | 문자열 그대로. 본체 192자·속성 254자 초과 시 절단하지 않음 |
| i.database_type → engine | 분류 라우팅; APPSERVER_PRODUCTNAME | 1절 표로 분류를 고르고 엔진 표기는 제품명에 보존. 같은 값을 다른 속성에 복제하지 않음 |
| i.db_type_id → db_type_id | 엔진 구분값 확인 | 이름 원천은 database_type. 관측 숫자를 분류 ID로 사용하지 않음 |
| i.host_name → host_name | 호스트 연결 조사 | 장치 이름 대체 조인에 사용하지 않음. 엔진 전용 호스트 속성 후보는 미결 |
| i.appcomp_fk → appcomp_fk | Component 경유 장치 탐색 | a.appcomp_pk와 조인. Instance 자체 ID가 아님 |
| i.database_count → database_count | 속성 미결·관계 건수 검증 | Target 속성 미선정. ISSUE-11 |
| i.connection_count → connection_count | 속성 미결 | 집계 시점·범위와 Target 속성 미확정. ISSUE-11 |
| i.is_default_instance → is_default_instance | 속성 미결 | Boolean 원천 보존. APPSERVER_ISPLACEHOLDER와 의미가 다름. ISSUE-11 |
| r.resource_pk → resource_pk | 동일 Resource 연결 확인 | source_pk와 일치; 별도 ACTCI 생성 안 함 |
| r.identifier → resource_identifier | APPSERVER_KEYNAME | `database_instance\|<이름>\|<호스트>` 원문 보존. ACTCI.GUID 식별자 정책은 ISSUE-11 |
| r.notes → source_description | ACTCI.DESCRIPTION | 문자열 그대로; 빈 문자열과 NULL 구분 유지. 1024자 초과 시 절단하지 않음 |
| r.last_discovered → resource_last_discovered | 사용하지 않음 | 두 서버 전건 NULL. 이 컬럼만으로는 LASTSCANDT를 채울 수 없다 |
| r.last_changed → resource_last_changed | ACTCI.LASTSCANDT | 3절 참조. Instance 레코드의 갱신 시각이며 CHANGEDATE 대체용은 아니다 |
| r.details.version → version_text | APPSERVER_PRODUCTVERSION | JSON 키가 없으면 NULL. 원문 보존; 254자 초과 시 절단하지 않음 |
| a.name → appcomp_name | Component 연결 확인 | Instance 이름·분류에 사용하지 않음. 엔진과 무관한 이름이 관측됨 |
| a.json.products[0].install_path → install_path | DATABASESERVER_HOME | 설치 경로의 유일한 원천. 없으면 NULL |
| a.json.products[0].version | 사용하지 않음 | Instance Resource 버전과 불일치 관측. 본체 버전은 r.details.version만 사용 |
| a.device_fk → host_source_pk | Instance→장치 `RELATION.RUNSON` | 실제 장치 h.device_pk와 일치할 때만 관계 생성 |
| a.last_changed / h.last_discovered | 연결 대상 시각 확인 | Instance 발견 시각으로 사용하지 않음 |
| h.device_pk → matched_host_pk | 장치 연결 성공 확인 | 원천 FK 보유와 실제 조인 성공을 구분 |

Resource JSON의 나머지 키는 [원천 구조](../../../knowledge/device42/database-model.md)에 분석했다.
정규 컬럼과 중복인 database_type·default_instance는 중복 매핑하지 않는다.
주소·CPU·메모리·시작 시각·메모리 상태의 Target 대응은 ISSUE-11이다.

## 3. 본체 매핑

공통 컬럼·타입·필수값은 [ACTCI](../actci.md)를 따른다.
ACTCINUM은 `D42:DATABASEINSTANCE:<i.databaseinstance_pk>`다.

| Target 컬럼 | 한글명 | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- |
| ACTCINAME | 실제 구성 품목 이름 | 직접 | `i.dbinstance_name` | 조회 결과 source_name; Component 이름으로 대체하지 않음 |
| DESCRIPTION | 설명 | 직접 | `r.notes` | 조회 결과 source_description; NULL 유지 |
| CLASSSTRUCTUREID | 분류 | 변환 | `i.database_type` + Maximo 분류 조회 | 1절 라우팅 표로 고른 분류의 ID |
| GUID | 발견 ID | 미결 | `r.identifier` | 원천 식별 문자열은 존재; Target 식별자 정책은 ISSUE-11 |
| LASTSCANDT | 최종 스캔 날짜 | 직접 | `r.last_changed` | `r.last_discovered`는 전건 NULL이고 이 컬럼은 Maximo에서 REQUIRED=1이라 적재가 불가능하다. Instance 레코드의 갱신 시각인 `r.last_changed`를 쓴다. 연결 Device의 발견 시각은 다른 개체의 값이라 쓰지 않는다 |

## 4. 속성 매핑

수집 속성의 대조 기준은 승격 대상 CI 분류가 제공하는 속성이다. 네 CI 분류의 합집합은
공통 8개 + 엔진 전용 11개이며 전부 대응 ACTCI 분류에 이미 있다. 근거는
[분류·스펙·관계 정의](../../../knowledge/maximo/db-classification-specs.md) 2·3절이다.
부모·템플릿 참조는 [ACTCISPEC](../actcispec.md)을 따른다.

적재 대상은 공통 8개 중 원천이 있는 5개다. 네 분류 모두 같은 5개를 쓴다.
SECTION은 모두 NULL, 값 컬럼은 ALNVALUE다.

| ASSETATTRID | 한글 의미 | 값 컬럼 | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- |
| APPSERVER_NAME | Instance 이름 | ALNVALUE | 직접 | `i.dbinstance_name` | 문자열 그대로 |
| APPSERVER_PRODUCTNAME | DB 제품명 | ALNVALUE | 직접 | `i.database_type` | 엔진 표기를 보존. 분류로 대체하지 않음 |
| APPSERVER_PRODUCTVERSION | DB 제품 버전 | ALNVALUE | 직접 | `CAST(r.details AS JSONB)->>'version'` | 원문 그대로; 키 미보유는 NULL. 최장 238자로 254자 안. 버전 번호만 추출하지 않음 |
| APPSERVER_KEYNAME | 원천 식별 키 | ALNVALUE | 직접 | `r.identifier` | 원문 그대로. `database_instance\|<이름>\|<호스트>` |
| DATABASESERVER_HOME | 설치 경로 | ALNVALUE | 직접 | `CAST(a.json AS JSONB)->'products'->0->>'install_path'` | Component 원천. 없으면 속성 생략 |

`APPSERVER_PRODUCTVERSION`은 CI 쪽에 있고 `APPSERVER_VERSIONSTRING`은 없다. 버전 원문을
승격 가능한 속성 하나에만 넣고 두 속성에 같은 값을 복제하지 않는다.

범용 분류로 `APP.DB.GENERICDATABASESERVER`를 쓰지 않는 이유도 같다. 두 분류는 관계 규칙이
완전히 같고 CLASSSPEC 차이가 `GENERICDATABASESERVER_GENERICTYPE` 하나뿐인데, 그 속성에 넣을
값이 `i.database_type`으로 `APPSERVER_PRODUCTNAME`과 같은 문자열이다. 대조는
[분류·스펙·관계 정의](../../../knowledge/maximo/db-classification-specs.md) 5절에 있다.

적재하지 않는 공통 속성이다.

| ASSETATTRID | 이유 |
| --- | --- |
| APPSERVER_VENDORNAME | D42에 벤더 원천이 없다. `database_type` 기준 고정표로 채울지 확정 전 |
| APPSERVER_EXECUTABLENAME | 원천 없음 |
| APPSERVER_STATUS | 원천 없음. NUMERIC이며 상태 코드 도메인도 미확인 |

엔진 전용 11개는 적재하지 않는다. `SQLSERVER_*` 6개와 `*_PORT` 2개는 원천이 없고,
`ORACLEINSTANCE_HOSTNAME`·`ORACLEINSTANCE_SID`·`DB2SERVER_NODENAME`은 후보 표본이 각 1건이며
의미 대응이 확실하지 않다. 추정으로 채우지 않는다. 판단 근거는
[수집 설계](../../../design/ci/databaseinstance.md) 6절이다.

Maximo 속성 템플릿 조회 SQL이다. 1절에서 선택한 네 분류와 일치하며,
각 CLASSSTRUCTUREID·ASSETATTRID·SECTION에 정확히 1건인 결과를 사용한다.

```sql
SELECT
    S.CLASSIFICATIONID,
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
WHERE S.CLASSIFICATIONID IN (
        'APP.DB.MSSQL.SQLSERVER',
        'APP.DB.DB2.DB2INSTANCE',
        'APP.DB.ORACLE.ORACLEINSTANCE',
        'APP.DB.DATABASESERVER'
      )
  AND C.ASSETATTRID IN (
        'APPSERVER_NAME',
        'APPSERVER_PRODUCTNAME',
        'APPSERVER_PRODUCTVERSION',
        'APPSERVER_KEYNAME',
        'DATABASESERVER_HOME'
      )
  AND C.SECTION IS NULL
  AND A.DATATYPE = 'ALN'
ORDER BY S.CLASSIFICATIONID, C.ASSETATTRID;
```

실행 결과 20건이다. 다섯 속성이 네 분류에 각각 있고 DISPLAYSEQUENCE도
`APPSERVER_KEYNAME` 10, `APPSERVER_NAME` 20, `APPSERVER_PRODUCTNAME` 21,
`APPSERVER_PRODUCTVERSION` 22, `DATABASESERVER_HOME` 28로 네 분류가 같다.
MANDATORY는 전부 0이다.
표시 순서·필수 여부는 공통 규약에 따라 적용 설정에서 조회한다.
DB 수·연결 수·기본 Instance 여부의 속성 대응은 ISSUE-11에 둔다.

## 5. 관계

이 문서가 Instance에서 출발하는 관계 정의의 정본이다.

| 관계 | 원천 연결 | 저장할 출발 / 도착 본체 | 상태 |
| --- | --- | --- | --- |
| Instance → DB | `d.databaseinstance_fk=i.databaseinstance_pk` | (database-instance, i.databaseinstance_pk) / (resource, d.database_pk) | 분류쌍 규칙 0건으로 적재 불가; `.68` 9쌍, `.35` 37쌍 |
| Instance → 장치 | `i.appcomp_fk=a.appcomp_pk AND a.device_fk=h.device_pk` | Instance / 연결된 Device | `RELATION.RUNSON`; `.68` 0건·미해결 1건, `.35` 3건 |

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
2026-09-16 원천 쌍은 `.68` 9건, `.35` 37건이다. `.68`은 `instance_id`·Resource
`root_resource_fk` 기준으로는 10쌍이다. 현재 SQL은 명시된 FK 기준만 사용하며,
불일치 1건의 관계 보강 여부는 ISSUE-11이다.

Instance→Device 원천 관계 조회 SQL:

```sql
SELECT
    I.databaseinstance_pk AS instance_source_pk,
    H.device_pk AS device_source_pk,
    I.database_type AS engine
FROM view_databaseinstance_v2 I
JOIN view_appcomp_v1 A
    ON A.appcomp_pk = I.appcomp_fk
JOIN view_device_v2 H
    ON H.device_pk = A.device_fk
ORDER BY I.databaseinstance_pk, H.device_pk;
```

원천은 Application Component를 경유하지만 저장할 양 끝은 DB Instance와 Device다.
Application Component를 이 관계의 별도 CI 노드로 만들지 않는다. 저장 방향은
`DB Instance --RELATION.RUNSON--> Device`이며, `host_name` 이름 조인은 사용하지 않는다.
2026-09-16 재조회 결과는 `.68` 0쌍·미해결 Instance 1건, `.35` 3쌍이다.

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
WHERE S.CLASSIFICATIONID IN (
        'APP.DB.MSSQL.SQLSERVER',
        'APP.DB.DB2.DB2INSTANCE',
        'APP.DB.ORACLE.ORACLEINSTANCE',
        'APP.DB.DATABASESERVER'
      )
  AND T.CLASSIFICATIONID IN ('SYS.COMPUTERSYSTEM', 'SYS.VIRTUALCOMPUTERSYSTEM')
  AND R.RELATIONNUM = 'RELATION.RUNSON';
```

사용하는 네 분류 모두 물리·가상 Device 대상 `RELATION.RUNSON` 1:1, 비포함 규칙을 가진다. `REVRELATIONSHIP`은 `APP.DB.ORACLE.ORACLEINSTANCE`만 1이고 나머지는 0이다.
규칙 설정 원문이며 저장 방향과 무관하다. Writer는 규칙 행의 존재만 확인하고
`ACTCIRELATION.SWAPPED`는 0으로 쓴다.

Instance→Database 방향은 위 SQL의 TARGET을 `APP.DB.DATABASE`로 바꾸면 **0건**이다.
`APP.DB.GENERICDATABASESERVER`를 넣어도 마찬가지다.
RELATIONNUM을 걸지 않아도 0건이다. `RELATION.CONTAINS` 코드가 존재해도 분류쌍 규칙이 없으면
MERGE 가드가 전건 거부하므로, MAS UI 등록 전에는 이 관계를 적재 대상으로 표시하지 않는다.

### 현재 관계 배치의 원천 조회

아래 조회는 원천 PK만 반환한다. Mapper가 Instance → Device의 식별자와 `RELATION.RUNSON`을 생성한다.

```sql
WITH computer AS (
    SELECT d.device_pk
    FROM view_device_v2 d
    WHERE
d.type IN ('physical', 'virtual')
AND (d.network_device = false OR d.network_device IS NULL)
AND (
    (d.type = 'physical' AND d.physicalsubtype IN ('Generic', 'Rackable', 'Blade', 'WorkStation', 'ThinClient', 'Laptop'))
    OR (d.type = 'virtual' AND d.virtualsubtype IN ('Internal VM', 'Amazon EC2 Instance', 'VMWare', 'Hyper-V'))
)
)
SELECT CAST(i.databaseinstance_pk AS varchar) AS source_pk,
       CAST(c.device_pk AS varchar) AS target_pk
FROM view_databaseinstance_v2 i
JOIN view_appcomp_v1 a ON a.appcomp_pk = i.appcomp_fk
JOIN computer c ON c.device_pk = a.device_fk
ORDER BY source_pk, target_pk
LIMIT 1000 OFFSET 0
```

## 6. 미결

[ISSUE-11](../../../open-issues.md#issue-11-actual-ci-분류속성관계와-식별자-매핑) — 추가 속성·관계 규칙·식별자·필수값 정책.
전체 원천 컬럼의 사용처와 미결 사유를 작성했다. 적재 실행·UI 검증은 하지 않았다.

이 문서 범위에서 남은 결정이다.

- `APPSERVER_VENDORNAME`을 `database_type` 고정표로 채울지 여부.
- 엔진 전용 속성 세 후보(`ORACLEINSTANCE_HOSTNAME`·`ORACLEINSTANCE_SID`·`DB2SERVER_NODENAME`)의 원천 확정.
- Instance→Database 관계의 분류쌍 규칙 등록.
