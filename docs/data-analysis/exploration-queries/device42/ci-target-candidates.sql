-- CI 본체 후보 View의 서버별 존재 건수와 PK 유일성을 비교한다.
-- 각 후보의 컬럼 형태는 sample 블록으로 확인한다.

-- name: ci-root-counts
SELECT 'view_appcomp_v1' AS view_name, COUNT(*) AS row_cnt, COUNT(appcomp_pk) AS pk_cnt, COUNT(DISTINCT appcomp_pk) AS distinct_pk_cnt FROM view_appcomp_v1
UNION ALL SELECT 'view_applicationgroup_v2', COUNT(*), COUNT(applicationgroup_pk), COUNT(DISTINCT applicationgroup_pk) FROM view_applicationgroup_v2
UNION ALL SELECT 'view_businessservice_v2', COUNT(*), COUNT(businessservice_pk), COUNT(DISTINCT businessservice_pk) FROM view_businessservice_v2
UNION ALL SELECT 'view_cloudinfrastructure_v2', COUNT(*), COUNT(cloudinfrastructure_pk), COUNT(DISTINCT cloudinfrastructure_pk) FROM view_cloudinfrastructure_v2
UNION ALL SELECT 'view_cloudinstance_v1', COUNT(*), COUNT(cloudinstance_pk), COUNT(DISTINCT cloudinstance_pk) FROM view_cloudinstance_v1
UNION ALL SELECT 'view_database_v2', COUNT(*), COUNT(database_pk), COUNT(DISTINCT database_pk) FROM view_database_v2
UNION ALL SELECT 'view_databaseinstance_v2', COUNT(*), COUNT(databaseinstance_pk), COUNT(DISTINCT databaseinstance_pk) FROM view_databaseinstance_v2
UNION ALL SELECT 'view_device_v2', COUNT(*), COUNT(device_pk), COUNT(DISTINCT device_pk) FROM view_device_v2
UNION ALL SELECT 'view_k8scluster_v2', COUNT(*), COUNT(k8scluster_pk), COUNT(DISTINCT k8scluster_pk) FROM view_k8scluster_v2
UNION ALL SELECT 'view_k8sdeployment_v2', COUNT(*), COUNT(k8sdeployment_pk), COUNT(DISTINCT k8sdeployment_pk) FROM view_k8sdeployment_v2
UNION ALL SELECT 'view_k8snode_v2', COUNT(*), COUNT(k8snode_pk), COUNT(DISTINCT k8snode_pk) FROM view_k8snode_v2
UNION ALL SELECT 'view_k8sservice_v2', COUNT(*), COUNT(k8sservice_pk), COUNT(DISTINCT k8sservice_pk) FROM view_k8sservice_v2
UNION ALL SELECT 'view_resource_v2', COUNT(*), COUNT(resource_pk), COUNT(DISTINCT resource_pk) FROM view_resource_v2
UNION ALL SELECT 'view_serviceinstance_v2', COUNT(*), COUNT(serviceinstance_pk), COUNT(DISTINCT serviceinstance_pk) FROM view_serviceinstance_v2
UNION ALL SELECT 'view_storagearray_v2', COUNT(*), COUNT(storagearray_pk), COUNT(DISTINCT storagearray_pk) FROM view_storagearray_v2
UNION ALL SELECT 'view_subnet_v1', COUNT(*), COUNT(subnet_pk), COUNT(DISTINCT subnet_pk) FROM view_subnet_v1
UNION ALL SELECT 'view_vlan_v1', COUNT(*), COUNT(vlan_pk), COUNT(DISTINCT vlan_pk) FROM view_vlan_v1
UNION ALL SELECT 'view_vrfgroup_v1', COUNT(*), COUNT(vrfgroup_pk), COUNT(DISTINCT vrfgroup_pk) FROM view_vrfgroup_v1
ORDER BY view_name;

-- name: appcomp-sample
SELECT * FROM view_appcomp_v1 ORDER BY appcomp_pk LIMIT 3;

-- name: applicationgroup-sample
SELECT * FROM view_applicationgroup_v2 ORDER BY applicationgroup_pk LIMIT 3;

-- name: businessservice-sample
SELECT * FROM view_businessservice_v2 ORDER BY businessservice_pk LIMIT 3;

-- name: cloudinfrastructure-sample
SELECT * FROM view_cloudinfrastructure_v2 ORDER BY cloudinfrastructure_pk LIMIT 3;

-- name: cloudinstance-sample
SELECT * FROM view_cloudinstance_v1 ORDER BY cloudinstance_pk LIMIT 3;

-- name: database-sample
SELECT * FROM view_database_v2 ORDER BY database_pk LIMIT 3;

-- name: databaseinstance-sample
SELECT * FROM view_databaseinstance_v2 ORDER BY databaseinstance_pk LIMIT 3;

-- name: device-sample
SELECT * FROM view_device_v2 ORDER BY device_pk LIMIT 3;

-- name: k8scluster-sample
SELECT * FROM view_k8scluster_v2 ORDER BY k8scluster_pk LIMIT 3;

-- name: k8sdeployment-sample
SELECT * FROM view_k8sdeployment_v2 ORDER BY k8sdeployment_pk LIMIT 3;

-- name: k8snode-sample
SELECT * FROM view_k8snode_v2 ORDER BY k8snode_pk LIMIT 3;

-- name: k8sservice-sample
SELECT * FROM view_k8sservice_v2 ORDER BY k8sservice_pk LIMIT 3;

-- name: resource-sample
SELECT * FROM view_resource_v2 ORDER BY resource_pk LIMIT 3;

-- name: serviceinstance-sample
SELECT * FROM view_serviceinstance_v2 ORDER BY serviceinstance_pk LIMIT 3;

-- name: storagearray-sample
SELECT * FROM view_storagearray_v2 ORDER BY storagearray_pk LIMIT 3;

-- name: subnet-sample
SELECT * FROM view_subnet_v1 ORDER BY subnet_pk LIMIT 3;

-- name: vlan-sample
SELECT * FROM view_vlan_v1 ORDER BY vlan_pk LIMIT 3;

-- name: vrfgroup-sample
SELECT * FROM view_vrfgroup_v1 ORDER BY vrfgroup_pk LIMIT 3;
