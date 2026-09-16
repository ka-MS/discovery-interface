-- DB Instance -> Application Component -> Device 경로와 Application Component의
-- 독립 CI 적합성을 판단하기 위한 현재 표본 조회다. JSON 원문은 출력하지 않는다.

-- name: db-instance-path
SELECT
    i.databaseinstance_pk AS instance_pk,
    i.dbinstance_name AS instance_name,
    i.database_type,
    i.host_name,
    i.appcomp_fk,
    a.name AS appcomp_name,
    a.application_category_name AS appcomp_category,
    a.device_fk,
    d.name AS device_name,
    d.type AS device_type,
    d.physicalsubtype,
    d.virtualsubtype,
    CASE WHEN d.device_pk IS NULL THEN false ELSE true END AS device_matched
FROM view_databaseinstance_v2 i
LEFT JOIN view_appcomp_v1 a ON a.appcomp_pk = i.appcomp_fk
LEFT JOIN view_device_v2 d ON d.device_pk = a.device_fk
ORDER BY i.databaseinstance_pk;

-- name: appcomp-summary
SELECT
    COALESCE(a.application_category_name, '(null)') AS category,
    COUNT(*) AS appcomp_count,
    COUNT(DISTINCT a.device_fk) AS device_count,
    COUNT(NULLIF(TRIM(a.name), '')) AS named_count,
    SUM(CASE WHEN a.device_fk IS NULL THEN 1 ELSE 0 END) AS no_device_fk,
    SUM(CASE WHEN d.device_pk IS NULL THEN 1 ELSE 0 END) AS no_matched_device
FROM view_appcomp_v1 a
LEFT JOIN view_device_v2 d ON d.device_pk = a.device_fk
GROUP BY COALESCE(a.application_category_name, '(null)')
ORDER BY appcomp_count DESC, category;

-- name: appcomp-list
SELECT
    a.appcomp_pk,
    a.name,
    a.application_category_name AS category,
    a.device_fk,
    d.name AS device_name,
    d.type AS device_type,
    COALESCE(
        CAST(a.json AS JSONB)->'products'->0->>'name',
        CAST(a.json AS JSONB)->'raw_data'->>'name',
        CAST(a.json AS JSONB)->'properties'->>'Server version'
    ) AS product_name,
    COALESCE(
        CAST(a.json AS JSONB)->'products'->0->>'version',
        CAST(a.json AS JSONB)->'properties'->>'Server number',
        CAST(a.json AS JSONB)->'properties'->>'JVM Version'
    ) AS product_version,
    COALESCE(
        CAST(a.json AS JSONB)->'products'->0->>'install_path',
        CAST(a.json AS JSONB)->'properties'->>'home_path'
    ) AS install_path,
    a.last_changed
FROM view_appcomp_v1 a
LEFT JOIN view_device_v2 d ON d.device_pk = a.device_fk
ORDER BY a.application_category_name, a.device_fk, a.appcomp_pk;

-- name: appcomp-per-device
SELECT
    a.device_fk,
    d.name AS device_name,
    COUNT(*) AS appcomp_count,
    COUNT(DISTINCT a.application_category_name) AS category_count
FROM view_appcomp_v1 a
LEFT JOIN view_device_v2 d ON d.device_pk = a.device_fk
GROUP BY a.device_fk, d.name
ORDER BY appcomp_count DESC, a.device_fk;

-- name: db-instance-path-coverage
SELECT
    COUNT(*) AS instance_count,
    COUNT(DISTINCT i.databaseinstance_pk) AS distinct_instance_count,
    COUNT(i.appcomp_fk) AS has_appcomp_fk,
    COUNT(a.appcomp_pk) AS matched_appcomp,
    COUNT(a.device_fk) AS has_device_fk,
    COUNT(d.device_pk) AS matched_device,
    COUNT(DISTINCT d.device_pk) AS distinct_devices
FROM view_databaseinstance_v2 i
LEFT JOIN view_appcomp_v1 a ON a.appcomp_pk = i.appcomp_fk
LEFT JOIN view_device_v2 d ON d.device_pk = a.device_fk;

-- name: db-instance-appcomp-reuse
SELECT
    i.appcomp_fk,
    COUNT(*) AS instance_count,
    COUNT(DISTINCT i.databaseinstance_pk) AS distinct_instances
FROM view_databaseinstance_v2 i
GROUP BY i.appcomp_fk
ORDER BY instance_count DESC, i.appcomp_fk;
