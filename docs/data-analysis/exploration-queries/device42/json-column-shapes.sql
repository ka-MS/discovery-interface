-- Device42 뷰의 JSON 컬럼(details, discovered_data, enrichai_details 등) 성격을
-- 확인한다. 정규 컬럼에 없는 값을 찾을 때 이 컬럼들을 뒤지게 되는데, 규격이
-- 없으므로 매번 실측해야 한다.
--
-- 설명은 이 헤더에만 둔다. 실행기가 블록 본문을 한 줄로 이어 붙여서, 첫
-- `-- name:` 뒤의 주석은 앞 블록 SQL 을 깨뜨린다.
--
-- `CAST(details AS VARCHAR) <> '{}'` 은 500 이다. 키 개수로 판정한다.
--   (SELECT COUNT(*) FROM jsonb_object_keys(d.details::jsonb)) > 0
-- `LATERAL jsonb_object_keys(...)` 와 `details->>'키'` 는 동작한다.
--
-- 블록
--   details-by-type    장비 타입별 details 보유와 최대 키 수
--   key-frequency      전체 details 키 출현 빈도
--   aws-keys           AWS EC2 장비의 키 집합
--   snmp-keys          네트워크 장비의 키 집합
--   windows-keys       Windows 장비의 키 집합
--   part-details-keys  파트 종류별 details 키
--   os-enrich          discovered_data 와 enrichai_details 대비
--   enrich-coverage    보강 데이터 충전율

-- name: details-by-type
WITH k AS (
    SELECT d.device_pk, d.type,
        (SELECT COUNT(*) FROM jsonb_object_keys(d.details::jsonb)) AS key_count
    FROM view_device_v2 d
)
SELECT type, COUNT(*) AS devices,
    SUM(CASE WHEN key_count > 0 THEN 1 ELSE 0 END) AS has_details,
    MAX(key_count) AS max_keys
FROM k
GROUP BY type
ORDER BY devices DESC;

-- name: key-frequency
SELECT k, COUNT(*) AS devices
FROM view_device_v2 d, LATERAL jsonb_object_keys(d.details::jsonb) AS k
GROUP BY k
ORDER BY devices DESC, k;

-- name: aws-keys
SELECT DISTINCT k
FROM view_device_v2 d, LATERAL jsonb_object_keys(d.details::jsonb) AS k
WHERE d.virtualsubtype_id = 2
ORDER BY k;

-- name: snmp-keys
SELECT DISTINCT k
FROM view_device_v2 d, LATERAL jsonb_object_keys(d.details::jsonb) AS k
WHERE d.network_device = true
ORDER BY k;

-- name: windows-keys
SELECT DISTINCT k
FROM view_device_v2 d, LATERAL jsonb_object_keys(d.details::jsonb) AS k
WHERE d.details->>'domain' IS NOT NULL
ORDER BY k;

-- name: part-details-keys
SELECT DISTINCT pm.type_name, k
FROM view_part_v1 p
JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk,
LATERAL jsonb_object_keys(p.details::jsonb) AS k
ORDER BY pm.type_name, k;

-- name: os-enrich
SELECT o.deviceos_pk,
    CAST(o.discovered_data AS VARCHAR) AS discovered_data,
    SUBSTR(CAST(o.enrichai_details AS VARCHAR), 1, 300) AS enrichai_details
FROM view_deviceos_v1 o
WHERE o.enrichai_details IS NOT NULL
LIMIT 3;

-- name: enrich-coverage
SELECT COUNT(*) AS rows_all,
    SUM(CASE WHEN (SELECT COUNT(*) FROM jsonb_object_keys(o.discovered_data::jsonb)) > 0
             THEN 1 ELSE 0 END) AS discovered_n,
    SUM(CASE WHEN (SELECT COUNT(*) FROM jsonb_object_keys(o.enrichai_details::jsonb)) > 0
             THEN 1 ELSE 0 END) AS enrich_n
FROM view_deviceos_v1 o;
