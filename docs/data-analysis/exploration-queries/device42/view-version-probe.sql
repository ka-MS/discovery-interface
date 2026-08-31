-- 뷰의 최고 버전을 확인한다. DOQL 은 카탈로그 조회를 막아 두어 뷰 목록을
-- 얻을 수 없으므로, 버전을 붙여 개별로 찔러 보는 수밖에 없다.
--
-- 사용법
--   블록 하나가 뷰 하나다. 실행이 성공하면 그 버전이 존재한다.
--   HTTP 500 이 나면 없는 것이다. 실행기가 해당 블록만 건너뛴다.
--   새 뷰를 확인할 때는 아래 형식으로 블록을 추가한다.
--
-- 관측 2026-08-31 기준 결과
--   _v2 존재: device, service, serviceinstance, physicalsubtype, parttype,
--             hardware, ipaddress, mountpoint
--   _v3 은 없다. 나머지 뷰는 _v1 뿐이다.
--
-- 규칙은 `../../knowledge/device42/views.md` 버전 규칙 절을 따른다.

-- name: probe-hardware-v2
SELECT * FROM view_hardware_v2 LIMIT 1;

-- name: probe-ipaddress-v2
SELECT * FROM view_ipaddress_v2 LIMIT 1;

-- name: probe-mountpoint-v2
SELECT * FROM view_mountpoint_v2 LIMIT 1;

-- name: probe-physicalsubtype-v2
SELECT * FROM view_physicalsubtype_v2 LIMIT 1;

-- name: probe-parttype-v2
SELECT * FROM view_parttype_v2 LIMIT 1;

-- name: probe-os-v2
SELECT * FROM view_os_v2 LIMIT 1;

-- name: probe-software-v2
SELECT * FROM view_software_v2 LIMIT 1;

-- name: probe-part-v2
SELECT * FROM view_part_v2 LIMIT 1;

-- name: probe-netport-v2
SELECT * FROM view_netport_v2 LIMIT 1;

-- name: probe-partmodel-v2
SELECT * FROM view_partmodel_v2 LIMIT 1;

-- name: probe-vendor-v2
SELECT * FROM view_vendor_v2 LIMIT 1;

-- name: probe-subnet-v2
SELECT * FROM view_subnet_v2 LIMIT 1;

-- name: probe-deviceos-v2
SELECT * FROM view_deviceos_v2 LIMIT 1;

-- name: probe-softwareinuse-v2
SELECT * FROM view_softwareinuse_v2 LIMIT 1;

-- name: probe-virtualsubtype-v2
SELECT * FROM view_virtualsubtype_v2 LIMIT 1;

-- name: probe-device-v3
SELECT * FROM view_device_v3 LIMIT 1;

-- name: probe-ipaddress-v3
SELECT * FROM view_ipaddress_v3 LIMIT 1;

-- name: probe-hardware-v3
SELECT * FROM view_hardware_v3 LIMIT 1;


-- 버전 간 행수 비교. 높은 버전이 더 많으면 낮은 버전이 행을 빠뜨린 것이다.
-- name: version-row-counts
SELECT 'view_ipaddress_v1' AS v, count(*) AS n FROM view_ipaddress_v1
UNION ALL SELECT 'view_ipaddress_v2',       count(*) FROM view_ipaddress_v2
UNION ALL SELECT 'view_hardware_v1',        count(*) FROM view_hardware_v1
UNION ALL SELECT 'view_hardware_v2',        count(*) FROM view_hardware_v2
UNION ALL SELECT 'view_mountpoint_v1',      count(*) FROM view_mountpoint_v1
UNION ALL SELECT 'view_mountpoint_v2',      count(*) FROM view_mountpoint_v2
UNION ALL SELECT 'view_physicalsubtype_v2', count(*) FROM view_physicalsubtype_v2
UNION ALL SELECT 'view_parttype_v2',        count(*) FROM view_parttype_v2
ORDER BY 1;


-- 마스터 뷰 전량. 코드값 목록이라 건수가 적다.
-- name: physicalsubtype-master
SELECT physicalsubtype_pk, physicalsubtype_name, verbose_name,
       storage_room, server_room, building, rack, chassis
FROM view_physicalsubtype_v2
ORDER BY physicalsubtype_pk;

-- name: parttype-master
SELECT parttype_pk, name, support_ports
FROM view_parttype_v2
ORDER BY parttype_pk;

-- virtualsubtype 은 마스터 뷰가 없다. device 에 등장한 값만 얻는다.
-- name: virtualsubtype-observed
SELECT virtualsubtype_id, virtualsubtype, count(*) AS n
FROM view_device_v2
GROUP BY virtualsubtype_id, virtualsubtype
ORDER BY virtualsubtype_id;
