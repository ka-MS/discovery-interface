# Database Instance

> 기준: 현재 코드 · 2026-09-21 문서 대조. 과거 원천·분류 관측은 2026-09-16, D42 .68 / .35 및 Maximo BLUDB다.
> 구현: [DatabaseInstanceCiImport](../../../../../src/main/java/com/itmsg/device42/pipeline/d42maximo/ci/databaseinstance/DatabaseInstanceCiImport.java) · [DatabaseInstanceCiQuery](../../../../../src/main/java/com/itmsg/device42/source/device42/ci/databaseinstance/DatabaseInstanceCiQuery.java) · [DatabaseInstanceCiMapper](../../../../../src/main/java/com/itmsg/device42/pipeline/d42maximo/ci/databaseinstance/DatabaseInstanceCiMapper.java) · [ActCiWriter](../../../../../src/main/java/com/itmsg/device42/target/maximo/ci/ActCiWriter.java)

## 1. 범위·분류

view_databaseinstance_v2 전 행을 조회한다. 엔진 필터는 없으며 Resource·Application Component는
LEFT JOIN하므로 보강 행이 없어도 Instance 후보는 유지한다.
독립 Database와 Application Component 본체는 이 작업에서 생성하지 않는다.

| database_type(trim 후) | Actual CI CLASSIFICATIONID |
| --- | --- |
| Microsoft SQL | APP.DB.MSSQL.SQLSERVER |
| DB2 | APP.DB.DB2.DB2INSTANCE |
| Oracle Database | APP.DB.ORACLE.ORACLEINSTANCE |
| 그 밖의 값·NULL | APP.DB.DATABASESERVER |

비교는 대소문자를 구분한다. 엔진별 분류가 선택된 후 그 정의가 없으면 해당 Instance를 건너뛴다.
분류 조회 실패를 범용 분류로 대체하지 않는다. CLASSSTRUCTUREID·CLASSSPECID는
[분류·스펙 명세](../classstructure.md)의 실제 로더 SQL과 유효성 규칙을 따른다.
CI.* 승격 대상은 이 ETL의 쓰기 대상이 아니다.

## 2. 원천 조회

COUNT는 view_databaseinstance_v2 전체를 센다. PAGE의 LIMIT/OFFSET은 예시 값이다.
이름·엔진·식별자·버전·설치 경로는 SQL에서 trim 후 빈 문자열을 NULL로 바꾼다.
notes는 그대로 읽는다.

```sql
SELECT COUNT(*) FROM view_databaseinstance_v2 i
```

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

| 원천 | 조인 | 현재 역할 |
| --- | --- | --- |
| view_databaseinstance_v2 i | 기준 행 | PK, Instance 이름, 엔진 |
| view_resource_v2 r | r.resource_pk=i.databaseinstance_pk | identifier, notes, last_changed, details.version |
| view_appcomp_v1 a | a.appcomp_pk=i.appcomp_fk | products[0].install_path; 첫 제품만 읽음 |

host_name·db_type_id·database_count·connection_count·is_default_instance는 현재 본체 SQL의 반환값이 아니다.
전체 원천 컬럼의 조사는 [원천 구조](../../../knowledge/device42/database-model.md)에 보존한다.
장비 연결은 본체의 보강 조회와 별도로 [관계 SQL](../relations.md)에서 처리한다.

## 3. 본체 매핑

공통 컬럼·시퀀스·MERGE·실패 처리는 [ACTCI](../actci.md)를 따른다.

| Target 컬럼 | 한글명 | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- |
| ACTCINUM | 실제 CI 번호 | 변환 | i.databaseinstance_pk | D42:DATABASEINSTANCE: 접두어 + PK |
| ACTCINAME | 실제 CI 이름 | 변환 | i.dbinstance_name | trim 후 빈 값 NULL. Component 이름으로 대체하지 않음 |
| CLASSSTRUCTUREID | 분류 | 변환 | i.database_type + Maximo 정의 | 1절에서 고른 분류의 ID |
| DESCRIPTION | 설명 | 직접 | r.notes | 원문, NULL·빈 문자열 구분 유지 |
| LASTSCANDT | 최종 스캔 날짜 | 변환 | r.last_changed | SourceTimestamp로 JVM 기본 시간대 변환. r.last_discovered나 장비 발견 시각으로 대체하지 않음 |
| CHANGEBY | 변경자 | 상수 | – | Device42 |
| CHANGEDATE | 변경 날짜 | 변환 | 페이지 매핑 시각 | 본체·스펙 공유 |
| LANGCODE | 언어 코드 | 상수 | – | KO |

GUID는 Writer가 쓰지 않는다. r.identifier는 APPSERVER_KEYNAME에만 사용한다.
길이 초과를 Mapper에서 절단하지 않는다. 시각 파싱 예외는 해당 Instance를 제외하며
시각 누락은 NULL로 전달한다. DB의 저장 거부 여부는 실제 제약에 따른다.

## 4. 속성 매핑

네 분류 모두 다음 다섯 속성만 매핑한다. SECTION=NULL, 값 컬럼=ALNVALUE다.
값이 없어도 정상 템플릿이 있으면 NULL 스펙 행을 생성한다.
템플릿·적용 설정이 없으면 생략하며 DB Instance에는 추가 속성 우회 경로가 없다.

| ASSETATTRID | 한글 의미 | 값 컬럼 | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- |
| APPSERVER_NAME | Instance 이름 | ALNVALUE | 변환 | i.dbinstance_name | trim, 공백은 NULL |
| APPSERVER_PRODUCTNAME | DB 제품명 | ALNVALUE | 변환 | i.database_type | trim, 공백은 NULL; 분류명으로 대체하지 않음 |
| APPSERVER_PRODUCTVERSION | DB 제품 버전 | ALNVALUE | 변환 | CAST(r.details AS JSONB)->>'version' | trim, 공백·키 미보유는 NULL; 버전 번호만 추출하지 않음 |
| APPSERVER_KEYNAME | 원천 식별 키 | ALNVALUE | 변환 | r.identifier | trim, 공백은 NULL; 본체 식별자와 별개 |
| DATABASESERVER_HOME | 설치 경로 | ALNVALUE | 변환 | CAST(a.json AS JSONB)->'products'->0->>'install_path' | trim, 공백·키 미보유는 NULL. 정상 템플릿이면 NULL 행으로 동기화 |

APPSERVER_VENDORNAME·APPSERVER_EXECUTABLENAME·APPSERVER_STATUS 및 엔진 전용 속성은 매핑하지 않는다.
원천 의미와 선택 이유는 [수집 설계](../../../design/ci/databaseinstance.md)에,
관측된 전체 등록 스펙과 순서·필수 여부는 [분류 조사](../../../knowledge/maximo/db-classification-specs.md)에 있다.
표시 순서·필수 여부를 코드 상수로 고정하지 않고 실행 시 Maximo 정의를 따른다.

## 5. 관계

현재 구현은 **DB Instance → Computer / RELATION.RUNSON** 하나다.
i.appcomp_fk → a.appcomp_pk → a.device_fk → Computer device_pk로 연결한다.
이름 기반 host_name 조인을 하지 않으며 양 끝은 Instance·Computer이고 Component는 중간 원천일 뿐이다.
대상 Computer 범위, 실제 COUNT/PAGE SQL, 식별자 변환은 [관계 통합 명세](../relations.md)를 따른다.
저장 시 실제 ACTCI 양 끝·RELATION·RELATIONRULES를 확인하고 SWAPPED=0을 쓴다.

Instance→Database는 구현하지 않았다. 조사 당시 FK 쌍이 있었다는 사실을 적재 구현으로 해석하지 않는다.
분류쌍 기준정보와 승격 정책은 [분류·관계 관측](../../../knowledge/maximo/db-classification-specs.md)을 참조한다.

## 6. 검증과 후속

2026-09-16 관측: Instance는 .68 1건 / .35 3건, 장비 연결은 .68 0쌍 / .35 3쌍이었다.
이는 당시 표본이며 현재 운영 건수가 아니다. 이번 문서 정비에서는 실제 DB 적재·UI를 검증하지 않았다.
자동 검증과 문서-코드 대조 범위는 [점검 기록](../../documentation-audit.md)을 따른다.
미수집 벤더·엔진 전용 속성, 독립 Database 관계·승격 등의 정책은 [ISSUE-11](../../../open-issues.md)에서 관리한다.
