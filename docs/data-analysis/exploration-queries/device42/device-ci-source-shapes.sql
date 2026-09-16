-- Device 통합 수집: 기존 Computer 조회가 사용하는 뷰의 컬럼 확인. 결과는 로컬에만 보관한다.
-- name: device-shape
SELECT * FROM view_device_v2 LIMIT 1;
-- name: hardware-shape
SELECT * FROM view_hardware_v2 LIMIT 1;
-- name: vendor-shape
SELECT * FROM view_vendor_v1 LIMIT 1;
-- name: part-shape
SELECT * FROM view_part_v1 LIMIT 1;
-- name: partmodel-shape
SELECT * FROM view_partmodel_v1 LIMIT 1;
-- name: netport-shape
SELECT * FROM view_netport_v1 LIMIT 1;
-- name: device-kinds
SELECT type,physicalsubtype,virtualsubtype,network_device,COUNT(*) AS cnt
FROM view_device_v2 GROUP BY type,physicalsubtype,virtualsubtype,network_device
ORDER BY type,physicalsubtype,virtualsubtype,network_device;
