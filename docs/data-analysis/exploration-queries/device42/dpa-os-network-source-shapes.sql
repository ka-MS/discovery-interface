-- DPAOS / DPATCPIP / DPANETPRINTER 매핑에 쓰는 Device42 원천 뷰의 컬럼 형태를
-- 확인한다. DOQL 은 카탈로그 조회가 막혀 있어 `SELECT *` 헤더 행으로 컬럼
-- 목록을 얻는다. 근거는 knowledge/device42/doql-constraints.md 다.

-- name: deviceos-shape
SELECT *
FROM view_deviceos_v1
LIMIT 1;

-- name: os-shape
SELECT *
FROM view_os_v1
LIMIT 1;

-- name: ipaddress-shape
SELECT *
FROM view_ipaddress_v2
LIMIT 1;

-- name: subnet-shape
SELECT *
FROM view_subnet_v1
LIMIT 1;

-- name: printer-device-shape
SELECT *
FROM view_device_v2
WHERE physicalsubtype = 'Network Printer'
LIMIT 1;

-- name: printer-part-shape
SELECT p.*
FROM view_part_v1 p
JOIN view_device_v2 d ON d.device_pk = p.device_fk
WHERE d.physicalsubtype = 'Network Printer'
LIMIT 1;
