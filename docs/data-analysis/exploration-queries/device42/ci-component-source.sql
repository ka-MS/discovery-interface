-- OS·Disk·Filesystem·IP 원천 뷰의 컬럼 목록과 표본 1행.
-- 카탈로그 조회가 막혀 있어 SELECT * 헤더로 컬럼을 확인한다.
-- 실행기는 첫 500에서 배치 전체를 중단하므로 실재가 확인된 뷰만 둔다.
-- 버전은 2026-09-15 .68에서 개별 확인했다. 목록은 knowledge/device42/views.md 참조.
-- name: os-shape
SELECT * FROM view_deviceos_v1 LIMIT 1;

-- name: os-product-shape
SELECT * FROM view_os_v1 LIMIT 1;

-- name: part-shape
SELECT * FROM view_part_v1 LIMIT 1;

-- name: partmodel-shape
SELECT * FROM view_partmodel_v1 LIMIT 1;

-- name: mount-shape
SELECT * FROM view_mountpoint_v2 LIMIT 1;

-- name: ip-shape
SELECT * FROM view_ipaddress_v2 LIMIT 1;

-- name: subnet-shape
SELECT * FROM view_subnet_v1 LIMIT 1;

-- name: netport-shape
SELECT * FROM view_netport_v1 LIMIT 1;
