-- DB Instance 보강 원천의 JSON 필드를 조회한다.

-- name: instance-resource-details
SELECT I.databaseinstance_pk,R.resource_pk,R.resource_name,R.vendor_resource_type,
R.identifier,R.notes,R.last_discovered,R.last_changed,
CAST(R.details AS JSONB)->>'version' AS version_text,
LENGTH(CAST(R.details AS JSONB)->>'version') AS version_length,
CAST(R.details AS JSONB)->>'database_type' AS detail_database_type,
CAST(R.details AS JSONB)->>'default_instance' AS detail_default_instance
FROM view_databaseinstance_v2 I LEFT JOIN view_resource_v2 R ON R.resource_pk=I.databaseinstance_pk
ORDER BY I.databaseinstance_pk;
