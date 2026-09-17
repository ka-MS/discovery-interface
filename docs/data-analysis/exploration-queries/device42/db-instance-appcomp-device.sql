-- DB Instance -> Application Component -> Device 경로와 Application Component의
-- 독립 CI 적합성을 판단하기 위한 현재 표본 조회다. JSON 원문은 출력하지 않는다.
--
-- 블록 사이에 주석을 넣으면 앞 블록의 SQL 뒤에 붙어 실행이 500으로 끝난다.
-- 블록 설명은 모두 이 머리말에 적는다.
--
-- db-instance-attribute-source   속성 매핑 후보의 원천 값과 길이. ALNVALUE 254자 초과 여부 확인.
--                                한 블록에서 r.details 를 세 번 이상 캐스팅하면 DOQL 이 500을
--                                돌려주므로 여기서는 version 만 읽는다.
-- db-instance-resource-json-keys Resource details JSON 의 키와 값. 엔진별로 키 구성이 다르다.
-- db-instance-appcomp-products   연결 Component 의 제품명·버전·설치 경로. 설치 경로의 유일한 원천.

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

-- name: db-instance-engines
SELECT i.database_type,
    COUNT(*) AS instance_count,
    COUNT(DISTINCT i.db_type_id) AS type_id_count,
    COUNT(DISTINCT i.appcomp_fk) AS appcomp_count
FROM view_databaseinstance_v2 i
GROUP BY i.database_type
ORDER BY instance_count DESC, i.database_type;

-- name: db-instance-attribute-source
SELECT i.databaseinstance_pk AS instance_pk,
    i.dbinstance_name,
    i.database_type,
    i.db_type_id,
    i.host_name,
    i.database_count,
    i.connection_count,
    i.is_default_instance,
    CAST(r.identifier AS VARCHAR(400)) AS resource_identifier,
    LENGTH(CAST(r.identifier AS VARCHAR(400))) AS identifier_len,
    LEFT(CAST(CAST(r.details AS JSONB)->>'version' AS VARCHAR(400)), 120) AS version_text,
    LENGTH(CAST(CAST(r.details AS JSONB)->>'version' AS VARCHAR(2000))) AS version_len
FROM view_databaseinstance_v2 i
LEFT JOIN view_resource_v2 r ON r.resource_pk = i.databaseinstance_pk
ORDER BY i.databaseinstance_pk;

-- name: db-instance-resource-json-keys
SELECT i.databaseinstance_pk AS instance_pk,
    i.database_type,
    k.key AS json_key,
    LEFT(CAST(CAST(r.details AS JSONB)->>k.key AS VARCHAR(400)), 120) AS json_value
FROM view_databaseinstance_v2 i
JOIN view_resource_v2 r ON r.resource_pk = i.databaseinstance_pk
CROSS JOIN LATERAL JSONB_OBJECT_KEYS(CAST(r.details AS JSONB)) AS k(key)
ORDER BY i.databaseinstance_pk, k.key;

-- name: db-instance-appcomp-products
SELECT i.databaseinstance_pk AS instance_pk,
    i.database_type,
    a.appcomp_pk,
    a.name AS appcomp_name,
    CAST(CAST(a.json AS JSONB)->'products'->0->>'name' AS VARCHAR(200)) AS product_name,
    CAST(CAST(a.json AS JSONB)->'products'->0->>'version' AS VARCHAR(100)) AS product_version,
    CAST(CAST(a.json AS JSONB)->'products'->0->>'install_path' AS VARCHAR(400)) AS install_path
FROM view_databaseinstance_v2 i
JOIN view_appcomp_v1 a ON a.appcomp_pk = i.appcomp_fk
ORDER BY i.databaseinstance_pk;
