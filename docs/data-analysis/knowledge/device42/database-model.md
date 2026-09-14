# DB·DB Instance 원천 구조

> 관측 2026-09-04 · Device42 192.168.2.68 / 192.168.1.35
> 재조회 SQL: 이 문서의 각 절. 원천은 DOQL View이며 물리 테이블 DDL은 확인되지 않았다.

## 행과 키

| View | 한 행 | .68 / .35 | 원천 키 | 참조 |
| --- | --- | ---: | --- | --- |
| view_database_v2 | Database 한 개 | 10 / 0 | database_pk | databaseinstance_fk → Instance |
| view_databaseinstance_v2 | DB 서버 소프트웨어 Instance 한 개 | 1 / 0 | databaseinstance_pk | appcomp_fk → Application Component |

두 View의 전체 컬럼 헤더는 양 서버에서 동일하다. .35는 헤더만 반환하므로 값·NULL·형식은 검증할 표본이 없다.
아래 NULL 수는 .68의 SQL NULL이며 DDL의 nullable 여부가 아니다.
형식은 반환 값과 CAST 검증 기준이다. 물리 타입·길이·제약으로 간주하지 않는다.

## Database: 전체 10컬럼

```sql
SELECT * FROM view_database_v2 ORDER BY database_pk;
```

| 컬럼 | 의미 | 관측 형식 / 값 | NULL / 10 |
| --- | --- | --- | ---: |
| database_pk | D42 DB 원천 키 | BIGINT 변환 가능; 표본 내 유일 | 0 |
| database_id | DB 내부 ID | BIGINT 변환 가능; 1~10. Instance 간 유일성은 미검증 | 0 |
| collate | 정렬 규칙 | 문자열; 최대 28자 | 0 |
| creation_date | DB 생성 시각 | 오프셋 포함 시각 표현; 최대 26자 | 0 |
| database_name | DB 이름 | 문자열; 최대 13자, 표본 내 유일 | 0 |
| databaseinstance_fk | Instance 참조 키 | BIGINT 변환 가능; 9건이 Instance PK와 일치 | 1 |
| instance_id | 별도 Instance 식별값 | BIGINT 변환 가능; 10건이 Instance PK와 일치 | 0 |
| compatibility_level | 호환성 수준 | INTEGER 변환 가능; 전건 140 | 0 |
| recovery_model | 복구 모드 | 문자열; SIMPLE / FULL | 0 |
| allocated_size | 할당 크기 | DECIMAL(30,5) 변환 가능; 7,642,624~27,076,132,864. 단위 미확인 | 0 |

database_id와 database_pk는 다른 값이다. 이름·database_id의 표본 유일성은 원천 식별자 정책의 근거가 되지 않는다.
SQL에서 `collate`는 `D."collate"`로 인용한다. compatibility_level을 제품 버전으로 해석하지 않는다.

## DB Instance: 전체 9컬럼

```sql
SELECT * FROM view_databaseinstance_v2 ORDER BY databaseinstance_pk;
```

| 컬럼 | 의미 | 관측 형식 / 값 | NULL / 1 |
| --- | --- | --- | ---: |
| databaseinstance_pk | D42 Instance 원천 키 | BIGINT 변환 가능 | 0 |
| dbinstance_name | Instance 이름 | 문자열; DEFAULT, 7자 | 0 |
| host_name | 호스트 이름 | 전건 NULL; 형식 미검증 | 1 |
| appcomp_fk | Application Component 참조 | BIGINT 변환 가능; 연결 1건 | 0 |
| database_type | DB 엔진 이름 | 문자열; Microsoft SQL, 13자 | 0 |
| db_type_id | DB 엔진 구분값 | INTEGER 변환 가능; 1. 다른 값과의 대응 미검증 | 0 |
| database_count | 보고된 DB 개수 | BIGINT 변환 가능; 9, FK 연결 DB 수와 일치 | 0 |
| connection_count | 보고된 연결 수 | BIGINT 변환 가능; 170. 집계 시점·방식 미확인 | 0 |
| is_default_instance | 기본 Instance 여부 | BOOLEAN 변환 가능; t | 0 |

Instance View 자체에는 식별 문자열·발견 시각·버전 컬럼이 없다. Resource에서 보강할 수 있는 값과 구분한다.

## 보강 원천

```sql
SELECT R.*
FROM view_resource_v2 R
WHERE EXISTS (
    SELECT 1 FROM view_database_v2 D WHERE D.database_pk = R.resource_pk
)
OR EXISTS (
    SELECT 1 FROM view_databaseinstance_v2 I WHERE I.databaseinstance_pk = R.resource_pk
)
ORDER BY R.resource_pk;
```

| 연결 | 관측 .68 / .35 | 의미 |
| --- | --- | --- |
| D.database_pk = R.resource_pk | 10 / 0; 이름 전건 일치 | Database와 Resource는 동일 본체 |
| I.databaseinstance_pk = R.resource_pk | 1 / 0; 이름 일치 | Instance와 Resource는 동일 본체 |
| I.appcomp_fk = A.appcomp_pk | 1 / 0; PK·이름은 다름 | Component를 Instance와 동일 본체로 간주할 근거는 아님 |
| A.device_fk = H.device_pk | 0 / 0 | 연결된 Component의 device_fk가 NULL |

| Resource 컬럼 | DB / Instance 관측 | 보강 의미 |
| --- | --- | --- |
| identifier | 10 / 1건 보유 | 원천 식별 문자열. Maximo GUID 적용 정책과 별개 |
| notes | DB 10건·Instance 1건 모두 빈 문자열 | SQL NULL과 구분; 비어 있지 않은 메모 표본 없음 |
| last_discovered | 양쪽 전건 NULL | 발견 시각 원천은 있으나 필수 Target 값을 채우지 못함 |
| last_changed | 10 / 1건 보유 | Resource 변경 시각. 발견 시각이 아님 |
| vendor_resource_type | Database / Database Instance | 관측 유형; 엔진별 분류 조건으로 사용하지 않음 |
| root_resource_fk | DB 10건이 Instance Resource와 일치 | databaseinstance_fk와 일치 범위가 다름 |
| details | DB 10건 / Instance 1건 | JSON 보강 값; 아래 키별 관측 참조 |

DB details의 database_id·collate·size·compatibility_level·recovery_model_desc는
정규 컬럼의 database_id·collate·allocated_size·compatibility_level·recovery_model과 10건 모두 일치한다.
이는 조회 결과를 파싱해 비교한 값이며, JSON 경로를 별도 DB 본체로 적재하는 근거가 아니다.

Instance details의 관측 키:

| 키 | 확인 내용 |
| --- | --- |
| version | 엔진·빌드·OS 설명을 포함한 버전 문자열, 238자. 단순 제품 버전 번호가 아님 |
| database_type / default_instance | 정규 컬럼의 엔진 / 기본 Instance 여부와 일치 |
| network_address | 주소 문자열 보유. Device와의 식별 관계는 미확인 |
| server_cpu_count / server_hyperthread_ratio | 숫자 보유. Instance 고유 자원인지 호스트 자원인지 미확인 |
| server_virtual_memory_kb / server_physical_memory_kb / server_available_physical_memory_kb | 숫자 보유. 키에 kb가 있으나 단위·집계 범위는 미검증 |
| sqlserver_start_time | 한국어 오전·오후를 포함한 시각 문자열. 발견 시각이 아님 |
| server_system_memory_state_desc | 메모리 상태 설명 문자열 |

## Instance 참조값 차이

```sql
SELECT
    D.database_pk,
    D.databaseinstance_fk,
    D.instance_id,
    R.root_resource_fk,
    I.databaseinstance_pk AS fk_match,
    J.databaseinstance_pk AS instance_id_match
FROM view_database_v2 D
LEFT JOIN view_resource_v2 R ON R.resource_pk = D.database_pk
LEFT JOIN view_databaseinstance_v2 I ON I.databaseinstance_pk = D.databaseinstance_fk
LEFT JOIN view_databaseinstance_v2 J ON J.databaseinstance_pk = D.instance_id
ORDER BY D.database_pk;
```

.68의 9건은 세 참조값이 같다. 나머지 1건은 databaseinstance_fk만 NULL이고
instance_id·root_resource_fk는 기존 Instance를 가리킨다. FK 기준은 9쌍, instance_id 기준은 10쌍이다.
따라서 기존 표현인 ‘Instance 없는 DB’는 ‘databaseinstance_fk가 없는 DB’로 한정해야 한다.
참조값 차이의 원인·우선순위와 관계 보강 여부는 [ISSUE-11](../../open-issues.md#issue-11-actual-ci-분류속성관계와-식별자-매핑)에 둔다.
