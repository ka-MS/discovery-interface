-- DB·DB Instance 유형별 원천 투영과 조인 손실을 검증한다.
-- 적재용 식별자·분류·발견 시각은 생성하지 않는다. 미매핑 행도 반환한다.

-- name: ci-database-source
SELECT D.database_pk AS source_pk,D.database_name AS source_name,D.database_id,
D.creation_date,D."collate" AS collation,D.compatibility_level,D.recovery_model,D.allocated_size,
R.resource_pk,R.identifier AS resource_identifier,R.vendor_resource_type,R.vendor_resource_subtype,
R.last_discovered AS resource_last_discovered,R.last_changed AS resource_last_changed,
D.databaseinstance_fk AS instance_source_pk,I.database_type AS instance_engine,
I.dbinstance_name AS instance_name
FROM view_database_v2 D
LEFT JOIN view_resource_v2 R ON R.resource_pk=D.database_pk
LEFT JOIN view_databaseinstance_v2 I ON I.databaseinstance_pk=D.databaseinstance_fk
ORDER BY D.database_pk;

-- name: ci-db-instance-source
SELECT I.databaseinstance_pk AS source_pk,I.dbinstance_name AS source_name,I.database_type AS engine,
I.host_name,I.database_count,I.connection_count,I.is_default_instance,
I.appcomp_fk,A.name AS appcomp_name,A.device_fk AS host_source_pk,
A.last_changed AS appcomp_last_changed,H.last_discovered AS host_last_discovered,
H.device_pk AS matched_host_pk
FROM view_databaseinstance_v2 I
LEFT JOIN view_appcomp_v1 A ON A.appcomp_pk=I.appcomp_fk
LEFT JOIN view_device_v2 H ON H.device_pk=A.device_fk
ORDER BY I.databaseinstance_pk;

-- name: ci-db-instance-database-edges
SELECT I.databaseinstance_pk AS instance_source_pk,D.database_pk AS database_source_pk,
I.database_type AS engine
FROM view_databaseinstance_v2 I JOIN view_database_v2 D ON D.databaseinstance_fk=I.databaseinstance_pk
ORDER BY I.databaseinstance_pk,D.database_pk;

-- name: ci-db-source-audit
SELECT R.vendor_resource_type,R.vendor_resource_subtype,I.database_type AS instance_engine,
COUNT(*) AS DB_ROWS,COUNT(DISTINCT D.database_pk) AS SOURCE_KEYS,
COUNT(R.resource_pk) AS RESOURCE_MATCH,COUNT(I.databaseinstance_pk) AS INSTANCE_MATCH,
COUNT(NULLIF(TRIM(R.identifier),'')) AS IDENTIFIER_ROWS,
COUNT(R.last_discovered) AS SCAN_ROWS,
MAX(LENGTH(D.database_name)) AS MAX_NAME_LENGTH,
MAX(LENGTH(CAST(D.creation_date AS TEXT))) AS MAX_CREATION_LENGTH,
MIN(D.database_id) AS MIN_DB_ID,MAX(D.database_id) AS MAX_DB_ID
FROM view_database_v2 D
LEFT JOIN view_resource_v2 R ON R.resource_pk=D.database_pk
LEFT JOIN view_databaseinstance_v2 I ON I.databaseinstance_pk=D.databaseinstance_fk
GROUP BY R.vendor_resource_type,R.vendor_resource_subtype,I.database_type;

-- name: ci-db-instance-audit
SELECT I.database_type,COUNT(*) AS INSTANCE_ROWS,COUNT(DISTINCT I.databaseinstance_pk) AS SOURCE_KEYS,
COUNT(A.appcomp_pk) AS APPCOMP_MATCH,COUNT(H.device_pk) AS HOST_MATCH,
COUNT(NULLIF(TRIM(I.host_name),'')) AS HOST_NAME_ROWS,
MAX(LENGTH(I.dbinstance_name)) AS MAX_NAME_LENGTH,
COUNT(A.last_changed) AS APPCOMP_CHANGED_ROWS,
SUM(I.database_count) AS REPORTED_DB_COUNT
FROM view_databaseinstance_v2 I LEFT JOIN view_appcomp_v1 A ON A.appcomp_pk=I.appcomp_fk
LEFT JOIN view_device_v2 H ON H.device_pk=A.device_fk GROUP BY I.database_type;

-- name: ci-db-value-conversion
SELECT COUNT(*) AS ROW_CNT,COUNT(CAST(database_id AS DECIMAL(30,5))) AS NUMERIC_DB_IDS,
COUNT(CAST(creation_date AS TEXT)) AS CREATION_TEXT_ROWS,
MAX(LENGTH(CAST(creation_date AS TEXT))) AS MAX_CREATION_LENGTH,
SUM(CASE WHEN LENGTH(CAST(creation_date AS TEXT))>254 THEN 1 ELSE 0 END) AS CREATION_OVERFLOW
FROM view_database_v2;
