-- DB와 DB Instance의 전체 원천 컬럼 및 보강 원천의 관계를 조사한다.

-- name: database-columns
SELECT * FROM view_database_v2 ORDER BY database_pk;

-- name: databaseinstance-columns
SELECT * FROM view_databaseinstance_v2 ORDER BY databaseinstance_pk;

-- name: database-resources
SELECT R.* FROM view_resource_v2 R JOIN view_database_v2 D ON D.database_pk=R.resource_pk ORDER BY R.resource_pk;

-- name: instance-components
SELECT A.* FROM view_appcomp_v1 A JOIN view_databaseinstance_v2 I ON I.appcomp_fk=A.appcomp_pk ORDER BY A.appcomp_pk;

-- name: instance-resources
SELECT R.* FROM view_resource_v2 R JOIN view_databaseinstance_v2 I ON I.databaseinstance_pk=R.resource_pk ORDER BY R.resource_pk;

-- name: database-key-audit
SELECT COUNT(*) AS db_rows,COUNT(DISTINCT D.database_pk) AS distinct_pk,
COUNT(DISTINCT D.database_name) AS distinct_name,COUNT(DISTINCT D.database_id) AS distinct_db_id,
COUNT(R.resource_pk) AS resource_match,COUNT(D.databaseinstance_fk) AS instance_fk_rows,
COUNT(I.databaseinstance_pk) AS instance_match,
SUM(CASE WHEN R.resource_pk IS NOT NULL AND D.database_name=R.resource_name THEN 1 ELSE 0 END) AS same_resource_name,
COUNT(R.last_discovered) AS scan_rows
FROM view_database_v2 D LEFT JOIN view_resource_v2 R ON R.resource_pk=D.database_pk
LEFT JOIN view_databaseinstance_v2 I ON I.databaseinstance_pk=D.databaseinstance_fk;

-- name: database-instance-count-audit
SELECT I.databaseinstance_pk,I.dbinstance_name,I.database_count,COUNT(D.database_pk) AS linked_databases
FROM view_databaseinstance_v2 I LEFT JOIN view_database_v2 D ON D.databaseinstance_fk=I.databaseinstance_pk
GROUP BY I.databaseinstance_pk,I.dbinstance_name,I.database_count ORDER BY I.databaseinstance_pk;

-- name: database-instance-reference-audit
SELECT COUNT(*) AS db_rows,COUNT(D.databaseinstance_fk) AS fk_rows,COUNT(D.instance_id) AS instance_id_rows,
COUNT(I.databaseinstance_pk) AS fk_match,COUNT(J.databaseinstance_pk) AS instance_id_match,
SUM(CASE WHEN D.databaseinstance_fk=D.instance_id THEN 1 ELSE 0 END) AS equal_values,
SUM(CASE WHEN D.databaseinstance_fk IS NULL AND D.instance_id IS NOT NULL THEN 1 ELSE 0 END) AS id_without_fk
FROM view_database_v2 D
LEFT JOIN view_databaseinstance_v2 I ON I.databaseinstance_pk=D.databaseinstance_fk
LEFT JOIN view_databaseinstance_v2 J ON J.databaseinstance_pk=D.instance_id;

-- name: database-cast-audit
SELECT COUNT(*) AS db_rows,
COUNT(CAST(database_pk AS BIGINT)) AS pk_bigint,
COUNT(CAST(database_id AS BIGINT)) AS db_id_bigint,
COUNT(CAST(databaseinstance_fk AS BIGINT)) AS fk_bigint,
COUNT(CAST(instance_id AS BIGINT)) AS instance_id_bigint,
COUNT(CAST(compatibility_level AS INTEGER)) AS compatibility_integer,
COUNT(CAST(allocated_size AS DECIMAL(30,5))) AS size_numeric,
MIN(allocated_size) AS min_size,MAX(allocated_size) AS max_size,
MIN(creation_date) AS min_creation,MAX(creation_date) AS max_creation,
MAX(LENGTH(database_name)) AS name_length,MAX(LENGTH("collate")) AS collation_length,
MAX(LENGTH(recovery_model)) AS recovery_length
FROM view_database_v2;

-- name: instance-cast-audit
SELECT COUNT(*) AS instance_rows,
COUNT(CAST(databaseinstance_pk AS BIGINT)) AS pk_bigint,
COUNT(CAST(appcomp_fk AS BIGINT)) AS appcomp_bigint,
COUNT(CAST(db_type_id AS INTEGER)) AS type_integer,
COUNT(CAST(database_count AS BIGINT)) AS db_count_bigint,
COUNT(CAST(connection_count AS BIGINT)) AS connections_bigint,
COUNT(CAST(is_default_instance AS BOOLEAN)) AS default_boolean,
MAX(LENGTH(dbinstance_name)) AS name_length,MAX(LENGTH(database_type)) AS engine_length
FROM view_databaseinstance_v2;

-- name: database-root-resource-audit
SELECT D.database_pk,D.databaseinstance_fk,D.instance_id,R.root_resource_fk,
P.resource_pk AS root_match,P.resource_name AS root_name,P.vendor_resource_type AS root_type,
P.vendor_resource_subtype AS root_subtype,P.last_discovered AS root_scan,
CASE WHEN P.identifier IS NOT NULL THEN 1 ELSE 0 END AS root_has_identifier
FROM view_database_v2 D
JOIN view_resource_v2 R ON R.resource_pk=D.database_pk
LEFT JOIN view_resource_v2 P ON P.resource_pk=R.root_resource_fk
ORDER BY D.database_pk;

-- name: database-column-null-audit
SELECT COUNT(*) AS row_cnt,COUNT(database_pk) AS database_pk,COUNT(database_id) AS database_id,
COUNT("collate") AS collation,COUNT(creation_date) AS creation_date,COUNT(database_name) AS database_name,
COUNT(databaseinstance_fk) AS databaseinstance_fk,COUNT(instance_id) AS instance_id,
COUNT(compatibility_level) AS compatibility_level,COUNT(recovery_model) AS recovery_model,
COUNT(allocated_size) AS allocated_size
FROM view_database_v2;

-- name: instance-column-null-audit
SELECT COUNT(*) AS row_cnt,COUNT(databaseinstance_pk) AS databaseinstance_pk,COUNT(dbinstance_name) AS dbinstance_name,
COUNT(host_name) AS host_name,COUNT(appcomp_fk) AS appcomp_fk,COUNT(database_type) AS database_type,
COUNT(db_type_id) AS db_type_id,COUNT(database_count) AS database_count,COUNT(connection_count) AS connection_count,
COUNT(is_default_instance) AS is_default_instance
FROM view_databaseinstance_v2;

-- name: resource-column-null-audit
SELECT R.vendor_resource_type,COUNT(*) AS row_cnt,COUNT(R.identifier) AS identifier,
COUNT(R.notes) AS notes,COUNT(R.last_discovered) AS last_discovered,COUNT(R.last_changed) AS last_changed
FROM view_resource_v2 R
WHERE EXISTS (SELECT 1 FROM view_database_v2 D WHERE D.database_pk=R.resource_pk)
OR EXISTS (SELECT 1 FROM view_databaseinstance_v2 I WHERE I.databaseinstance_pk=R.resource_pk)
GROUP BY R.vendor_resource_type;
