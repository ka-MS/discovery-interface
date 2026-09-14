-- CI 원천 후보의 값 충전율·길이와 식별/관계 조건을 조회한다.
-- 표본 값 대신 집계만 반환한다. 필드 목록은 각 SELECT에서 변경한다.

-- name: ci-values-device
SELECT 'view_device_v2' AS SOURCE_VIEW,'name' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(name AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(name AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(name AS TEXT))) AS MAX_LENGTH FROM view_device_v2
UNION ALL
SELECT 'view_device_v2' AS SOURCE_VIEW,'serial_no' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(serial_no AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(serial_no AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(serial_no AS TEXT))) AS MAX_LENGTH FROM view_device_v2
UNION ALL
SELECT 'view_device_v2' AS SOURCE_VIEW,'asset_no' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(asset_no AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(asset_no AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(asset_no AS TEXT))) AS MAX_LENGTH FROM view_device_v2
UNION ALL
SELECT 'view_device_v2' AS SOURCE_VIEW,'uuid' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(uuid AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(uuid AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(uuid AS TEXT))) AS MAX_LENGTH FROM view_device_v2
UNION ALL
SELECT 'view_device_v2' AS SOURCE_VIEW,'last_discovered' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(last_discovered AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(last_discovered AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(last_discovered AS TEXT))) AS MAX_LENGTH FROM view_device_v2
UNION ALL
SELECT 'view_device_v2' AS SOURCE_VIEW,'last_changed' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(last_changed AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(last_changed AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(last_changed AS TEXT))) AS MAX_LENGTH FROM view_device_v2
UNION ALL
SELECT 'view_device_v2' AS SOURCE_VIEW,'notes' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(notes AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(notes AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(notes AS TEXT))) AS MAX_LENGTH FROM view_device_v2
UNION ALL
SELECT 'view_device_v2' AS SOURCE_VIEW,'total_cpus' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(total_cpus AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(total_cpus AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(total_cpus AS TEXT))) AS MAX_LENGTH FROM view_device_v2
UNION ALL
SELECT 'view_device_v2' AS SOURCE_VIEW,'cpu_speed' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(cpu_speed AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(cpu_speed AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(cpu_speed AS TEXT))) AS MAX_LENGTH FROM view_device_v2
UNION ALL
SELECT 'view_device_v2' AS SOURCE_VIEW,'ram' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(ram AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(ram AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(ram AS TEXT))) AS MAX_LENGTH FROM view_device_v2
UNION ALL
SELECT 'view_device_v2' AS SOURCE_VIEW,'bios_release_date' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(bios_release_date AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(bios_release_date AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(bios_release_date AS TEXT))) AS MAX_LENGTH FROM view_device_v2;

-- name: ci-values-resource
SELECT 'view_resource_v2' AS SOURCE_VIEW,'resource_name' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(resource_name AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(resource_name AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(resource_name AS TEXT))) AS MAX_LENGTH FROM view_resource_v2
UNION ALL
SELECT 'view_resource_v2' AS SOURCE_VIEW,'identifier' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(identifier AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(identifier AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(identifier AS TEXT))) AS MAX_LENGTH FROM view_resource_v2
UNION ALL
SELECT 'view_resource_v2' AS SOURCE_VIEW,'last_discovered' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(last_discovered AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(last_discovered AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(last_discovered AS TEXT))) AS MAX_LENGTH FROM view_resource_v2
UNION ALL
SELECT 'view_resource_v2' AS SOURCE_VIEW,'last_changed' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(last_changed AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(last_changed AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(last_changed AS TEXT))) AS MAX_LENGTH FROM view_resource_v2
UNION ALL
SELECT 'view_resource_v2' AS SOURCE_VIEW,'notes' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(notes AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(notes AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(notes AS TEXT))) AS MAX_LENGTH FROM view_resource_v2;

-- name: ci-values-database
SELECT 'view_database_v2' AS SOURCE_VIEW,'database_name' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(database_name AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(database_name AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(database_name AS TEXT))) AS MAX_LENGTH FROM view_database_v2
UNION ALL
SELECT 'view_database_v2' AS SOURCE_VIEW,'database_id' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(database_id AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(database_id AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(database_id AS TEXT))) AS MAX_LENGTH FROM view_database_v2
UNION ALL
SELECT 'view_database_v2' AS SOURCE_VIEW,'creation_date' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(creation_date AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(creation_date AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(creation_date AS TEXT))) AS MAX_LENGTH FROM view_database_v2
UNION ALL
SELECT 'view_database_v2' AS SOURCE_VIEW,'collate' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST("collate" AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST("collate" AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST("collate" AS TEXT))) AS MAX_LENGTH FROM view_database_v2
UNION ALL
SELECT 'view_database_v2' AS SOURCE_VIEW,'compatibility_level' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(compatibility_level AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(compatibility_level AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(compatibility_level AS TEXT))) AS MAX_LENGTH FROM view_database_v2
UNION ALL
SELECT 'view_database_v2' AS SOURCE_VIEW,'recovery_model' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(recovery_model AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(recovery_model AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(recovery_model AS TEXT))) AS MAX_LENGTH FROM view_database_v2
UNION ALL
SELECT 'view_database_v2' AS SOURCE_VIEW,'allocated_size' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(allocated_size AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(allocated_size AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(allocated_size AS TEXT))) AS MAX_LENGTH FROM view_database_v2;

-- name: ci-values-databaseinstance
SELECT 'view_databaseinstance_v2' AS SOURCE_VIEW,'dbinstance_name' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(dbinstance_name AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(dbinstance_name AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(dbinstance_name AS TEXT))) AS MAX_LENGTH FROM view_databaseinstance_v2
UNION ALL
SELECT 'view_databaseinstance_v2' AS SOURCE_VIEW,'host_name' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(host_name AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(host_name AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(host_name AS TEXT))) AS MAX_LENGTH FROM view_databaseinstance_v2
UNION ALL
SELECT 'view_databaseinstance_v2' AS SOURCE_VIEW,'database_type' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(database_type AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(database_type AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(database_type AS TEXT))) AS MAX_LENGTH FROM view_databaseinstance_v2
UNION ALL
SELECT 'view_databaseinstance_v2' AS SOURCE_VIEW,'database_count' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(database_count AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(database_count AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(database_count AS TEXT))) AS MAX_LENGTH FROM view_databaseinstance_v2
UNION ALL
SELECT 'view_databaseinstance_v2' AS SOURCE_VIEW,'connection_count' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(connection_count AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(connection_count AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(connection_count AS TEXT))) AS MAX_LENGTH FROM view_databaseinstance_v2;

-- name: ci-values-appcomp
SELECT 'view_appcomp_v1' AS SOURCE_VIEW,'name' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(name AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(name AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(name AS TEXT))) AS MAX_LENGTH FROM view_appcomp_v1
UNION ALL
SELECT 'view_appcomp_v1' AS SOURCE_VIEW,'last_changed' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(last_changed AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(last_changed AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(last_changed AS TEXT))) AS MAX_LENGTH FROM view_appcomp_v1;

-- name: ci-values-cloudinstance
SELECT 'view_cloudinstance_v1' AS SOURCE_VIEW,'instance_name' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(instance_name AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(instance_name AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(instance_name AS TEXT))) AS MAX_LENGTH FROM view_cloudinstance_v1
UNION ALL
SELECT 'view_cloudinstance_v1' AS SOURCE_VIEW,'instance_id' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(instance_id AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(instance_id AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(instance_id AS TEXT))) AS MAX_LENGTH FROM view_cloudinstance_v1
UNION ALL
SELECT 'view_cloudinstance_v1' AS SOURCE_VIEW,'date_modified' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(date_modified AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(date_modified AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(date_modified AS TEXT))) AS MAX_LENGTH FROM view_cloudinstance_v1;

-- name: ci-values-serviceinstance
SELECT 'view_serviceinstance_v2' AS SOURCE_VIEW,'service_path' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(service_path AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(service_path AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(service_path AS TEXT))) AS MAX_LENGTH FROM view_serviceinstance_v2
UNION ALL
SELECT 'view_serviceinstance_v2' AS SOURCE_VIEW,'last_updated' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(last_updated AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(last_updated AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(last_updated AS TEXT))) AS MAX_LENGTH FROM view_serviceinstance_v2
UNION ALL
SELECT 'view_serviceinstance_v2' AS SOURCE_VIEW,'first_detected' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(first_detected AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(first_detected AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(first_detected AS TEXT))) AS MAX_LENGTH FROM view_serviceinstance_v2
UNION ALL
SELECT 'view_serviceinstance_v2' AS SOURCE_VIEW,'state' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(state AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(state AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(state AS TEXT))) AS MAX_LENGTH FROM view_serviceinstance_v2
UNION ALL
SELECT 'view_serviceinstance_v2' AS SOURCE_VIEW,'startmode' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(startmode AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(startmode AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(startmode AS TEXT))) AS MAX_LENGTH FROM view_serviceinstance_v2;

-- name: ci-values-subnet
SELECT 'view_subnet_v1' AS SOURCE_VIEW,'name' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(name AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(name AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(name AS TEXT))) AS MAX_LENGTH FROM view_subnet_v1
UNION ALL
SELECT 'view_subnet_v1' AS SOURCE_VIEW,'network' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(network AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(network AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(network AS TEXT))) AS MAX_LENGTH FROM view_subnet_v1
UNION ALL
SELECT 'view_subnet_v1' AS SOURCE_VIEW,'mask_bits' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(mask_bits AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(mask_bits AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(mask_bits AS TEXT))) AS MAX_LENGTH FROM view_subnet_v1
UNION ALL
SELECT 'view_subnet_v1' AS SOURCE_VIEW,'last_changed' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(last_changed AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(last_changed AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(last_changed AS TEXT))) AS MAX_LENGTH FROM view_subnet_v1;

-- name: ci-values-vlan
SELECT 'view_vlan_v1' AS SOURCE_VIEW,'name' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(name AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(name AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(name AS TEXT))) AS MAX_LENGTH FROM view_vlan_v1
UNION ALL
SELECT 'view_vlan_v1' AS SOURCE_VIEW,'number' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(number AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(number AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(number AS TEXT))) AS MAX_LENGTH FROM view_vlan_v1
UNION ALL
SELECT 'view_vlan_v1' AS SOURCE_VIEW,'last_edited' AS FIELD,COUNT(*) AS ROW_CNT,
COUNT(NULLIF(TRIM(CAST(last_edited AS TEXT)),'')) AS VALUE_ROWS,
COUNT(DISTINCT NULLIF(TRIM(CAST(last_edited AS TEXT)),'')) AS DISTINCT_VALUES,
MAX(LENGTH(CAST(last_edited AS TEXT))) AS MAX_LENGTH FROM view_vlan_v1;

-- name: ci-database-host-chain
SELECT COALESCE(I.database_type,'(no instance)') AS DATABASE_TYPE,COUNT(*) AS DATABASES,
COUNT(I.databaseinstance_pk) AS INSTANCE_MATCH,COUNT(A.appcomp_pk) AS APPCOMP_MATCH,
COUNT(H.device_pk) AS HOST_MATCH,COUNT(DISTINCT I.databaseinstance_pk) AS INSTANCES,COUNT(DISTINCT H.device_pk) AS HOSTS
FROM view_database_v2 D LEFT JOIN view_databaseinstance_v2 I ON I.databaseinstance_pk=D.databaseinstance_fk
LEFT JOIN view_appcomp_v1 A ON A.appcomp_pk=I.appcomp_fk
LEFT JOIN view_device_v2 H ON H.device_pk=A.device_fk GROUP BY I.database_type;

-- name: ci-device-type-units
SELECT type,physicalsubtype,virtualsubtype,ram_size_type,hz,COUNT(*) AS ROW_CNT,
COUNT(total_cpus) AS CPU_ROWS,COUNT(ram) AS RAM_ROWS
FROM view_device_v2 GROUP BY type,physicalsubtype,virtualsubtype,ram_size_type,hz
ORDER BY type,physicalsubtype,virtualsubtype,ram_size_type,hz;

-- name: ci-device-host-cardinality
SELECT COUNT(*) AS CHILD_ROWS,COUNT(P.device_pk) AS MATCHED_HOSTS,
COUNT(DISTINCT P.device_pk) AS DISTINCT_HOSTS
FROM view_device_v2 D LEFT JOIN view_device_v2 P ON P.device_pk=D.virtual_host_device_fk
WHERE D.virtual_host_device_fk IS NOT NULL;

-- name: ci-instance-host-cardinality
SELECT COUNT(*) AS INSTANCE_ROWS,COUNT(H.device_pk) AS HOST_MATCH,
COUNT(DISTINCT H.device_pk) AS DISTINCT_HOSTS
FROM view_databaseinstance_v2 I LEFT JOIN view_appcomp_v1 A ON A.appcomp_pk=I.appcomp_fk
LEFT JOIN view_device_v2 H ON H.device_pk=A.device_fk;

-- name: ci-device-resource-id-collision
SELECT COUNT(*) AS SAME_NUMERIC_PK
FROM view_device_v2 D JOIN view_resource_v2 R ON R.resource_pk=D.device_pk;

-- name: ci-overlap-same-row
SELECT 'database' AS KIND,COUNT(*) AS SOURCE_ROWS,
SUM(CASE WHEN R.resource_pk IS NOT NULL AND R.resource_name=D.database_name THEN 1 ELSE 0 END) AS SAME_ROW
FROM view_database_v2 D LEFT JOIN view_resource_v2 R ON R.resource_pk=D.database_pk
UNION ALL
SELECT 'k8scluster',COUNT(*),SUM(CASE WHEN R.resource_pk IS NOT NULL AND R.identifier=D.identifier AND R.resource_name=D.name THEN 1 ELSE 0 END)
FROM view_k8scluster_v2 D LEFT JOIN view_resource_v2 R ON R.resource_pk=D.k8scluster_pk
UNION ALL
SELECT 'k8sdeployment',COUNT(*),SUM(CASE WHEN R.resource_pk IS NOT NULL AND R.identifier=D.identifier AND R.resource_name=D.name THEN 1 ELSE 0 END)
FROM view_k8sdeployment_v2 D LEFT JOIN view_resource_v2 R ON R.resource_pk=D.k8sdeployment_pk
UNION ALL
SELECT 'k8snode',COUNT(*),SUM(CASE WHEN R.resource_pk IS NOT NULL AND R.identifier=D.identifier AND R.resource_name=D.name THEN 1 ELSE 0 END)
FROM view_k8snode_v2 D LEFT JOIN view_resource_v2 R ON R.resource_pk=D.k8snode_pk
UNION ALL
SELECT 'k8sservice',COUNT(*),SUM(CASE WHEN R.resource_pk IS NOT NULL AND R.identifier=D.identifier AND R.resource_name=D.name THEN 1 ELSE 0 END)
FROM view_k8sservice_v2 D LEFT JOIN view_resource_v2 R ON R.resource_pk=D.k8sservice_pk;
