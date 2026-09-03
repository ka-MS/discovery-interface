-- CI 본체 후보 사이의 FK 충전율과 실제 조인 성공 건수를 비교한다.
-- 원천 행이 0건인 서버도 같은 파일로 재조회한다.

-- name: ci-host-relations
SELECT 'appcomp-to-device' AS relation_name,
       COUNT(*) AS source_rows,
       SUM(CASE WHEN a.device_fk IS NOT NULL THEN 1 ELSE 0 END) AS fk_rows,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_device_v2 d WHERE d.device_pk = a.device_fk) THEN 1 ELSE 0 END) AS matched_rows
FROM view_appcomp_v1 a
UNION ALL
SELECT 'appcomp-to-resource', COUNT(*),
       SUM(CASE WHEN a.resource_fk IS NOT NULL THEN 1 ELSE 0 END),
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_resource_v2 r WHERE r.resource_pk = a.resource_fk) THEN 1 ELSE 0 END)
FROM view_appcomp_v1 a
UNION ALL
SELECT 'cloudinstance-to-device', COUNT(*),
       SUM(CASE WHEN c.device_fk IS NOT NULL THEN 1 ELSE 0 END),
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_device_v2 d WHERE d.device_pk = c.device_fk) THEN 1 ELSE 0 END)
FROM view_cloudinstance_v1 c
UNION ALL
SELECT 'serviceinstance-to-device', COUNT(*),
       SUM(CASE WHEN s.device_fk IS NOT NULL THEN 1 ELSE 0 END),
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_device_v2 d WHERE d.device_pk = s.device_fk) THEN 1 ELSE 0 END)
FROM view_serviceinstance_v2 s
ORDER BY relation_name;

-- name: ci-database-relations
SELECT 'database-to-instance' AS relation_name,
       COUNT(*) AS source_rows,
       SUM(CASE WHEN d.databaseinstance_fk IS NOT NULL THEN 1 ELSE 0 END) AS fk_rows,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_databaseinstance_v2 i WHERE i.databaseinstance_pk = d.databaseinstance_fk) THEN 1 ELSE 0 END) AS matched_rows
FROM view_database_v2 d
UNION ALL
SELECT 'instance-to-appcomp', COUNT(*),
       SUM(CASE WHEN i.appcomp_fk IS NOT NULL THEN 1 ELSE 0 END),
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_appcomp_v1 a WHERE a.appcomp_pk = i.appcomp_fk) THEN 1 ELSE 0 END)
FROM view_databaseinstance_v2 i
ORDER BY relation_name;

-- name: ci-k8s-relations
SELECT 'deployment-to-cluster' AS relation_name,
       COUNT(*) AS source_rows,
       SUM(CASE WHEN d.k8scluster_fk IS NOT NULL THEN 1 ELSE 0 END) AS fk_rows,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_k8scluster_v2 c WHERE c.k8scluster_pk = d.k8scluster_fk) THEN 1 ELSE 0 END) AS matched_rows
FROM view_k8sdeployment_v2 d
UNION ALL
SELECT 'node-to-cluster', COUNT(*),
       SUM(CASE WHEN n.k8scluster_fk IS NOT NULL THEN 1 ELSE 0 END),
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_k8scluster_v2 c WHERE c.k8scluster_pk = n.k8scluster_fk) THEN 1 ELSE 0 END)
FROM view_k8snode_v2 n
UNION ALL
SELECT 'service-to-cluster', COUNT(*),
       SUM(CASE WHEN s.k8scluster_fk IS NOT NULL THEN 1 ELSE 0 END),
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_k8scluster_v2 c WHERE c.k8scluster_pk = s.k8scluster_fk) THEN 1 ELSE 0 END)
FROM view_k8sservice_v2 s
ORDER BY relation_name;

-- name: ci-resource-relations
SELECT 'resource-to-cloud' AS relation_name,
       COUNT(*) AS source_rows,
       SUM(CASE WHEN r.cloudinfrastructure_fk IS NOT NULL THEN 1 ELSE 0 END) AS fk_rows,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_cloudinfrastructure_v2 c WHERE c.cloudinfrastructure_pk = r.cloudinfrastructure_fk) THEN 1 ELSE 0 END) AS matched_rows
FROM view_resource_v2 r
UNION ALL
SELECT 'resource-to-root-resource', COUNT(*),
       SUM(CASE WHEN r.root_resource_fk IS NOT NULL THEN 1 ELSE 0 END),
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_resource_v2 p WHERE p.resource_pk = r.root_resource_fk) THEN 1 ELSE 0 END)
FROM view_resource_v2 r
UNION ALL
SELECT 'storagearray-to-resource', COUNT(*),
       SUM(CASE WHEN s.resource_fk IS NOT NULL THEN 1 ELSE 0 END),
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_resource_v2 r WHERE r.resource_pk = s.resource_fk) THEN 1 ELSE 0 END)
FROM view_storagearray_v2 s
ORDER BY relation_name;

-- name: ci-network-relations
SELECT 'subnet-to-parent-subnet' AS relation_name,
       COUNT(*) AS source_rows,
       SUM(CASE WHEN s.parent_subnet_fk IS NOT NULL THEN 1 ELSE 0 END) AS fk_rows,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_subnet_v1 p WHERE p.subnet_pk = s.parent_subnet_fk) THEN 1 ELSE 0 END) AS matched_rows
FROM view_subnet_v1 s
UNION ALL
SELECT 'subnet-to-vlan', COUNT(*),
       SUM(CASE WHEN s.parent_vlan_fk IS NOT NULL THEN 1 ELSE 0 END),
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_vlan_v1 v WHERE v.vlan_pk = s.parent_vlan_fk) THEN 1 ELSE 0 END)
FROM view_subnet_v1 s
UNION ALL
SELECT 'subnet-to-vrf', COUNT(*),
       SUM(CASE WHEN s.vrfgroup_fk IS NOT NULL THEN 1 ELSE 0 END),
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_vrfgroup_v1 v WHERE v.vrfgroup_pk = s.vrfgroup_fk) THEN 1 ELSE 0 END)
FROM view_subnet_v1 s
UNION ALL
SELECT 'vrf-to-cloud', COUNT(*),
       SUM(CASE WHEN v.cloudinfrastructure_fk IS NOT NULL THEN 1 ELSE 0 END),
       SUM(CASE WHEN EXISTS (SELECT 1 FROM view_cloudinfrastructure_v2 c WHERE c.cloudinfrastructure_pk = v.cloudinfrastructure_fk) THEN 1 ELSE 0 END)
FROM view_vrfgroup_v1 v
ORDER BY relation_name;
