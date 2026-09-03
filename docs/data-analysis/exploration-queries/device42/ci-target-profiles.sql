-- CI 본체 후보가 독립 개체인지 다른 본체의 반복 상세인지 판단할 때 쓰는 분포다.

-- name: appcomp-profile
SELECT COALESCE(application_category_name, '(null)') AS category,
       COUNT(*) AS row_cnt,
       COUNT(DISTINCT device_fk) AS devices,
       COUNT(DISTINCT resource_fk) AS resources
FROM view_appcomp_v1
GROUP BY application_category_name
ORDER BY row_cnt DESC, category;

-- name: cloudinstance-profile
SELECT COUNT(*) AS row_cnt,
       COUNT(DISTINCT cloudinstance_pk) AS distinct_pk,
       COUNT(DISTINCT device_fk) AS devices,
       COUNT(DISTINCT instance_id) AS instance_ids,
       SUM(CASE WHEN device_fk IS NULL THEN 1 ELSE 0 END) AS no_device
FROM view_cloudinstance_v1;

-- name: database-profile
SELECT COUNT(*) AS row_cnt,
       COUNT(DISTINCT database_pk) AS distinct_pk,
       COUNT(DISTINCT databaseinstance_fk) AS instances,
       COUNT(DISTINCT database_id) AS database_ids,
       COUNT(DISTINCT database_name) AS database_names,
       SUM(CASE WHEN databaseinstance_fk IS NULL THEN 1 ELSE 0 END) AS no_instance
FROM view_database_v2;

-- name: databaseinstance-profile
SELECT COUNT(*) AS row_cnt,
       COUNT(DISTINCT databaseinstance_pk) AS distinct_pk,
       COUNT(DISTINCT appcomp_fk) AS appcomps,
       COUNT(DISTINCT dbinstance_name) AS instance_names,
       SUM(CASE WHEN appcomp_fk IS NULL THEN 1 ELSE 0 END) AS no_appcomp
FROM view_databaseinstance_v2;

-- name: resource-categories
SELECT category,
       vendor_resource_type,
       vendor_resource_subtype,
       COUNT(*) AS row_cnt
FROM view_resource_v2
GROUP BY category, vendor_resource_type, vendor_resource_subtype
ORDER BY row_cnt DESC;

-- name: serviceinstance-profile
SELECT COUNT(*) AS row_cnt,
       COUNT(DISTINCT serviceinstance_pk) AS distinct_pk,
       COUNT(DISTINCT device_fk) AS devices,
       COUNT(DISTINCT service_fk) AS services,
       COUNT(DISTINCT softwareinuse_fk) AS software_records,
       SUM(CASE WHEN device_fk IS NULL THEN 1 ELSE 0 END) AS no_device
FROM view_serviceinstance_v2;

-- name: serviceinstance-states
SELECT COALESCE(state, '(null)') AS state,
       COALESCE(startmode, '(null)') AS startmode,
       COUNT(*) AS row_cnt
FROM view_serviceinstance_v2
GROUP BY state, startmode
ORDER BY row_cnt DESC, state, startmode;

-- name: applicationgroup-status
SELECT COALESCE(status, '(null)') AS status,
       COALESCE(processing_status, '(null)') AS processing_status,
       COUNT(*) AS row_cnt
FROM view_applicationgroup_v2
GROUP BY status, processing_status
ORDER BY row_cnt DESC, status, processing_status;
