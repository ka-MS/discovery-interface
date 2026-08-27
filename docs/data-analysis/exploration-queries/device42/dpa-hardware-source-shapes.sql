-- DPA 하드웨어 자식 매핑에 쓰는 Device42 원천 뷰의 컬럼 형태를 확인한다.

-- name: mountpoint-shape
SELECT *
FROM view_mountpoint_v1
LIMIT 1;

-- name: netport-shape
SELECT *
FROM view_netport_v1
LIMIT 1;

-- name: gpu-part-shape
SELECT p.*
FROM view_part_v1 p
JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
WHERE pm.type_name = 'GPU'
LIMIT 1;

-- name: gpu-model-shape
SELECT pm.*
FROM view_partmodel_v1 pm
WHERE pm.type_name = 'GPU'
LIMIT 1;

-- name: vendor-shape
SELECT *
FROM view_vendor_v1
LIMIT 1;
