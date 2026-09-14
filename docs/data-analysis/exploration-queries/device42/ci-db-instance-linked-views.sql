-- ERD에 연결된 View별 실제 Instance 조인 건수를 확인한다.

-- name: instance-linked-view-counts
SELECT 'view_database_v2' AS linked_view,COUNT(*) AS linked_rows
FROM view_databaseinstance_v2 I JOIN view_database_v2 D ON D.databaseinstance_fk=I.databaseinstance_pk
UNION ALL
SELECT 'view_databaseconnection_v2',COUNT(*)
FROM view_databaseinstance_v2 I JOIN view_databaseconnection_v2 C ON C.databaseinstance_fk=I.databaseinstance_pk
UNION ALL
SELECT 'view_databaseinstance_custom_fields_v2',COUNT(*)
FROM view_databaseinstance_v2 I JOIN view_databaseinstance_custom_fields_v2 C ON C.databaseinstance_fk=I.databaseinstance_pk
UNION ALL
SELECT 'view_appcomp_v1',COUNT(*)
FROM view_databaseinstance_v2 I JOIN view_appcomp_v1 A ON A.appcomp_pk=I.appcomp_fk
UNION ALL
SELECT 'view_databasesize_v2',COUNT(*)
FROM view_databaseinstance_v2 I JOIN view_databasesize_v2 S ON S.databaseinstance_fk=I.databaseinstance_pk
UNION ALL
SELECT 'view_resource_v2',COUNT(*)
FROM view_databaseinstance_v2 I JOIN view_resource_v2 R ON R.resource_pk=I.databaseinstance_pk;

-- name: instance-engine-pairs
SELECT DISTINCT db_type_id,database_type FROM view_databaseinstance_v2 ORDER BY db_type_id;
