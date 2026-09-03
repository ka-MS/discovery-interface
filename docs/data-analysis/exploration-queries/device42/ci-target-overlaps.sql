-- 전용 View와 범용 본체 View가 같은 개체를 중복 표현하는지 확인한다.
-- PK가 서로 다른 체계일 수 있으므로 identifier와 이름도 함께 비교한다.

-- name: k8s-resource-overlap
SELECT 'cluster' AS candidate,
       COUNT(*) AS source_rows,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_resource_v2 r WHERE r.resource_pk = c.k8scluster_pk) THEN 1 ELSE 0 END) AS same_pk,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_resource_v2 r WHERE r.identifier = c.identifier) THEN 1 ELSE 0 END) AS same_identifier,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_resource_v2 r WHERE r.resource_name = c.name) THEN 1 ELSE 0 END) AS same_name
FROM view_k8scluster_v2 c
UNION ALL
SELECT 'deployment',
       COUNT(*) AS source_rows,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_resource_v2 r WHERE r.resource_pk = d.k8sdeployment_pk) THEN 1 ELSE 0 END) AS same_pk,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_resource_v2 r WHERE r.identifier = d.identifier) THEN 1 ELSE 0 END) AS same_identifier,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_resource_v2 r WHERE r.resource_name = d.name) THEN 1 ELSE 0 END) AS same_name
FROM view_k8sdeployment_v2 d
UNION ALL
SELECT 'node', COUNT(*),
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_resource_v2 r WHERE r.resource_pk = n.k8snode_pk) THEN 1 ELSE 0 END),
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_resource_v2 r WHERE r.identifier = n.identifier) THEN 1 ELSE 0 END),
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_resource_v2 r WHERE r.resource_name = n.name) THEN 1 ELSE 0 END)
FROM view_k8snode_v2 n
UNION ALL
SELECT 'service', COUNT(*),
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_resource_v2 r WHERE r.resource_pk = s.k8sservice_pk) THEN 1 ELSE 0 END),
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_resource_v2 r WHERE r.identifier = s.identifier) THEN 1 ELSE 0 END),
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_resource_v2 r WHERE r.resource_name = s.name) THEN 1 ELSE 0 END)
FROM view_k8sservice_v2 s
ORDER BY candidate;

-- name: database-resource-overlap
SELECT COUNT(*) AS source_rows,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_resource_v2 r WHERE r.resource_pk = d.database_pk) THEN 1 ELSE 0 END) AS same_pk,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_resource_v2 r WHERE r.resource_name = d.database_name) THEN 1 ELSE 0 END) AS same_name
FROM view_database_v2 d;

-- name: cloudinstance-device-cardinality
SELECT COUNT(*) AS cloud_rows,
       COUNT(DISTINCT device_fk) AS distinct_devices,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_device_v2 d WHERE d.device_pk = c.device_fk) THEN 1 ELSE 0 END) AS matched_devices,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_device_v2 d WHERE d.device_pk = c.cloudinstance_pk) THEN 1 ELSE 0 END) AS same_pk,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_device_v2 d WHERE d.name = c.instance_name) THEN 1 ELSE 0 END) AS same_name
FROM view_cloudinstance_v1 c;

-- name: dbinstance-appcomp-cardinality
SELECT COUNT(*) AS instance_rows,
       COUNT(DISTINCT appcomp_fk) AS distinct_appcomps,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_appcomp_v1 a WHERE a.appcomp_pk = i.appcomp_fk) THEN 1 ELSE 0 END) AS matched_appcomps,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_appcomp_v1 a WHERE a.appcomp_pk = i.databaseinstance_pk) THEN 1 ELSE 0 END) AS same_pk,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_appcomp_v1 a WHERE a.name = i.dbinstance_name) THEN 1 ELSE 0 END) AS same_name
FROM view_databaseinstance_v2 i;
