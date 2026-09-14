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

-- name: pk-uniqueness
WITH os AS (SELECT COUNT(*) AS n, COUNT(DISTINCT deviceos_pk) AS d FROM view_deviceos_v1),
     mnt AS (SELECT COUNT(*) AS n, COUNT(DISTINCT mountpoint_pk) AS d FROM view_mountpoint_v2),
     ipa AS (SELECT COUNT(*) AS n, COUNT(DISTINCT ipaddress_pk) AS d FROM view_ipaddress_v2),
     dsk AS (SELECT COUNT(*) AS n, COUNT(DISTINCT p.part_pk) AS d
             FROM view_part_v1 p JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
             WHERE pm.type_name = 'Hard Disk')
SELECT 'os' AS entity, n AS row_count, d AS distinct_pk FROM os
UNION ALL SELECT 'filesystem', n, d FROM mnt
UNION ALL SELECT 'ip', n, d FROM ipa
UNION ALL SELECT 'disk', n, d FROM dsk;

-- name: computer-attachment
WITH computer AS (
    SELECT d.device_pk FROM view_device_v2 d
    WHERE d.type IN ('physical', 'virtual')
      AND (d.network_device = false OR d.network_device IS NULL)
      AND (
          (d.type = 'physical' AND d.physicalsubtype IN
              ('Generic', 'Rackable', 'Blade', 'WorkStation', 'ThinClient', 'Laptop'))
          OR
          (d.type = 'virtual' AND d.virtualsubtype IN
              ('Internal VM', 'Amazon EC2 Instance', 'VMWare', 'Hyper-V'))
      )
), os AS (
    SELECT COUNT(*) AS total,
        SUM(CASE WHEN EXISTS (SELECT 1 FROM computer c WHERE c.device_pk = o.device_fk) THEN 1 ELSE 0 END) AS on_computer
    FROM view_deviceos_v1 o
), mnt AS (
    SELECT COUNT(*) AS total,
        SUM(CASE WHEN EXISTS (SELECT 1 FROM computer c WHERE c.device_pk = ANY(m.device_fks)) THEN 1 ELSE 0 END) AS on_computer
    FROM view_mountpoint_v2 m
), ipa AS (
    SELECT COUNT(*) AS total,
        SUM(CASE WHEN EXISTS (SELECT 1 FROM computer c WHERE c.device_pk = ANY(i.device_fks)) THEN 1 ELSE 0 END) AS on_computer
    FROM view_ipaddress_v2 i
), dsk AS (
    SELECT COUNT(*) AS total,
        SUM(CASE WHEN EXISTS (SELECT 1 FROM computer c WHERE c.device_pk = p.device_fk) THEN 1 ELSE 0 END) AS on_computer
    FROM view_part_v1 p JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
    WHERE pm.type_name = 'Hard Disk'
)
SELECT 'os' AS entity, total, on_computer FROM os
UNION ALL SELECT 'filesystem', total, on_computer FROM mnt
UNION ALL SELECT 'ip', total, on_computer FROM ipa
UNION ALL SELECT 'disk', total, on_computer FROM dsk;

-- name: os-value-coverage
SELECT COUNT(*) AS row_count, COUNT(o.os_name) AS has_name, COUNT(o.os_version) AS has_version,
    COUNT(o.os_version_no) AS has_version_no, COUNT(o.os_arch_name) AS has_arch,
    COUNT(o.os_fk) AS has_product, COUNT(o.eol) AS has_eol, COUNT(o.eos) AS has_eos,
    COUNT(DISTINCT o.device_fk) AS device_count
FROM view_deviceos_v1 o;

-- name: os-per-device
WITH per AS (SELECT o.device_fk AS dev, COUNT(*) AS n FROM view_deviceos_v1 o GROUP BY o.device_fk)
SELECT MAX(n) AS max_os_per_device, SUM(CASE WHEN n > 1 THEN 1 ELSE 0 END) AS device_with_multiple,
    COUNT(*) AS device_count FROM per;

-- name: mount-value-coverage
SELECT COUNT(*) AS row_count, COUNT(m.mountpoint) AS has_mountpoint, COUNT(m.identifier) AS has_identifier,
    COUNT(m.filesystem) AS has_filesystem, COUNT(m.fstype_name) AS has_fstype,
    COUNT(m.capacity) AS has_capacity, COUNT(m.free_capacity) AS has_free, COUNT(m.label) AS has_label
FROM view_mountpoint_v2 m;

-- name: ip-value-coverage
SELECT COUNT(*) AS row_count, COUNT(i.ip_address) AS has_ip, COUNT(i.netport_fk) AS has_port,
    COUNT(i.subnet_fk) AS has_subnet, COUNT(i.label) AS has_label, COUNT(i.type) AS has_type,
    COUNT(i.last_discovered) AS has_last_discovered,
    SUM(CASE WHEN i.is_shared THEN 1 ELSE 0 END) AS shared_count,
    COUNT(b.mask_bits) AS has_mask, COUNT(b.gateway) AS has_gateway
FROM view_ipaddress_v2 i
LEFT JOIN view_subnet_v1 b ON b.subnet_pk = i.subnet_fk;

-- name: disk-value-coverage
SELECT COUNT(*) AS row_count, COUNT(p.serial_no) AS has_serial, COUNT(p.pcount) AS has_pcount,
    COUNT(p.firmware) AS has_firmware, COUNT(pm.name) AS has_model, COUNT(pm.hdsize) AS has_size,
    COUNT(pm.hdsize_unit) AS has_size_unit, COUNT(pm.hddtype_name) AS has_hddtype,
    COUNT(pm.media_type_name) AS has_media, COUNT(pm.vendor_fk) AS has_vendor
FROM view_part_v1 p
JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
WHERE pm.type_name = 'Hard Disk';

-- name: disk-pcount-distribution
SELECT p.pcount, COUNT(*) AS row_count FROM view_part_v1 p
JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
WHERE pm.type_name = 'Hard Disk' GROUP BY p.pcount ORDER BY p.pcount;

-- name: array-fanout
WITH mnt AS (
    SELECT COUNT(*) AS rows_total,
        SUM(CASE WHEN m.device_fks IS NULL THEN 0 ELSE COALESCE(ARRAY_LENGTH(m.device_fks, 1), 0) END) AS pair_total,
        MAX(COALESCE(ARRAY_LENGTH(m.device_fks, 1), 0)) AS max_devices
    FROM view_mountpoint_v2 m
), ipa AS (
    SELECT COUNT(*) AS rows_total,
        SUM(CASE WHEN i.device_fks IS NULL THEN 0 ELSE COALESCE(ARRAY_LENGTH(i.device_fks, 1), 0) END) AS pair_total,
        MAX(COALESCE(ARRAY_LENGTH(i.device_fks, 1), 0)) AS max_devices
    FROM view_ipaddress_v2 i
)
SELECT 'filesystem' AS entity, rows_total, pair_total, max_devices FROM mnt
UNION ALL SELECT 'ip', rows_total, pair_total, max_devices FROM ipa;

-- name: ip-device-attachment
WITH per AS (
    SELECT i.ipaddress_pk AS pk,
        CASE WHEN i.device_fks IS NULL THEN 0 ELSE COALESCE(ARRAY_LENGTH(i.device_fks, 1), 0) END AS n
    FROM view_ipaddress_v2 i
)
SELECT n AS device_count, COUNT(*) AS ip_count FROM per GROUP BY n ORDER BY n;

-- name: mount-device-attachment
WITH per AS (
    SELECT m.mountpoint_pk AS pk,
        CASE WHEN m.device_fks IS NULL THEN 0 ELSE COALESCE(ARRAY_LENGTH(m.device_fks, 1), 0) END AS n
    FROM view_mountpoint_v2 m
)
SELECT n AS device_count, COUNT(*) AS mount_count FROM per GROUP BY n ORDER BY n;

-- name: scan-time-candidates
WITH mnt AS (
    SELECT COUNT(*) AS n, COUNT(first_added) AS has_first, COUNT(last_updated) AS has_updated
    FROM view_mountpoint_v2
), prt AS (
    SELECT COUNT(*) AS n, COUNT(p.first_added) AS has_first, COUNT(p.last_updated) AS has_updated
    FROM view_part_v1 p JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
    WHERE pm.type_name = 'Hard Disk'
), dos AS (
    SELECT COUNT(*) AS n, COUNT(first_added) AS has_first, COUNT(last_edited) AS has_updated
    FROM view_deviceos_v1
)
SELECT 'filesystem' AS entity, n AS row_count, has_first, has_updated FROM mnt
UNION ALL SELECT 'disk', n, has_first, has_updated FROM prt
UNION ALL SELECT 'os', n, has_first, has_updated FROM dos;

-- name: mount-fstype-distribution
SELECT m.fstype_name, COUNT(*) AS row_count FROM view_mountpoint_v2 m
GROUP BY m.fstype_name ORDER BY COUNT(*) DESC;

-- name: disk-hddtype-distribution
SELECT pm.hddtype_name, pm.hdsize_unit, COUNT(*) AS row_count
FROM view_part_v1 p JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
WHERE pm.type_name = 'Hard Disk'
GROUP BY pm.hddtype_name, pm.hdsize_unit ORDER BY COUNT(*) DESC;

-- name: os-arch-distribution
SELECT o.os_arch, o.os_arch_name, COUNT(*) AS row_count FROM view_deviceos_v1 o
GROUP BY o.os_arch, o.os_arch_name ORDER BY COUNT(*) DESC;

-- name: ip-type-distribution
SELECT i.type_id, i.type, i.available, i.is_public, COUNT(*) AS row_count
FROM view_ipaddress_v2 i GROUP BY i.type_id, i.type, i.available, i.is_public
ORDER BY COUNT(*) DESC;

-- name: os-product-distribution
SELECT m.category_name, v.name AS vendor, m.name AS product, COUNT(*) AS row_count
FROM view_deviceos_v1 o
JOIN view_os_v1 m ON m.os_pk = o.os_fk
LEFT JOIN view_vendor_v1 v ON v.vendor_pk = m.vendor_fk
GROUP BY m.category_name, v.name, m.name ORDER BY COUNT(*) DESC;
