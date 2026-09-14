-- Computer 수집 항목 조사. D42 원천만 조회하며 CI 포함·변환 정책을 확정하지 않는다.
-- physical/virtual 조사 표본에서 네트워크·프린터·전력설비·Docker Container를 분리한다.
-- 공유 IP/마운트의 연결 쌍을 보존한다. 행 수와 연결 쌍 수를 구분한다.

-- name: device-population
SELECT type, physicalsubtype, virtualsubtype, virtualsubtype_id, network_device, COUNT(*) AS rows
FROM view_device_v2
GROUP BY type, physicalsubtype, virtualsubtype, virtualsubtype_id, network_device
ORDER BY type, physicalsubtype, virtualsubtype;

-- name: computer-fields
WITH c AS (
    SELECT * FROM view_device_v2
    WHERE type IN ('physical', 'virtual')
      AND (network_device = false OR network_device IS NULL)
      AND (virtualsubtype IS NULL OR virtualsubtype <> 'Docker Container')
      AND (physicalsubtype IS NULL OR physicalsubtype NOT IN
          ('Network Printer', 'PDU', 'CRAC', 'UPS', 'Branch Circuit Power Meter', 'Power Unit', 'Environment Monitor'))
)
SELECT COUNT(*) AS rows,
    COUNT(NULLIF(TRIM(CAST(d.name AS varchar)), '')) AS has_name,
    COUNT(NULLIF(TRIM(CAST(d.serial_no AS varchar)), '')) AS has_serial,
    COUNT(NULLIF(TRIM(CAST(d.uuid AS varchar)), '')) AS has_uuid,
    COUNT(NULLIF(TRIM(CAST(h.name AS varchar)), '')) AS has_model,
    COUNT(NULLIF(TRIM(CAST(v.name AS varchar)), '')) AS has_vendor,
    COUNT(NULLIF(TRIM(CAST(d.last_discovered AS varchar)), '')) AS has_last_discovered,
    COUNT(NULLIF(TRIM(CAST(d.in_service AS varchar)), '')) AS has_in_service,
    COUNT(NULLIF(TRIM(CAST(b.name AS varchar)), '')) AS has_bios_vendor,
    COUNT(NULLIF(TRIM(CAST(d.bios_version AS varchar)), '')) AS has_bios_version,
    COUNT(NULLIF(TRIM(CAST(d.bios_release_date AS varchar)), '')) AS has_bios_date,
    COUNT(NULLIF(TRIM(CAST(d.ram AS varchar)), '')) AS has_ram,
    COUNT(NULLIF(TRIM(CAST(d.ram_size_type AS varchar)), '')) AS has_ram_unit,
    COUNT(NULLIF(TRIM(CAST(d.total_cpus AS varchar)), '')) AS has_cpu_count,
    COUNT(NULLIF(TRIM(CAST(d.core_per_cpu AS varchar)), '')) AS has_cores_per_cpu,
    COUNT(NULLIF(TRIM(CAST(d.cpu_speed AS varchar)), '')) AS has_cpu_speed
FROM c d LEFT JOIN view_hardware_v2 h ON h.hardware_pk=d.hardware_fk LEFT JOIN view_vendor_v1 v ON v.vendor_pk=h.vendor_fk LEFT JOIN view_vendor_v1 b ON b.vendor_pk=d.bios_vendor_fk;

-- name: cpu-fields
WITH c AS (
    SELECT * FROM view_device_v2
    WHERE type IN ('physical', 'virtual')
      AND (network_device = false OR network_device IS NULL)
      AND (virtualsubtype IS NULL OR virtualsubtype <> 'Docker Container')
      AND (physicalsubtype IS NULL OR physicalsubtype NOT IN
          ('Network Printer', 'PDU', 'CRAC', 'UPS', 'Branch Circuit Power Meter', 'Power Unit', 'Environment Monitor'))
)
SELECT COUNT(*) AS rows,
    COUNT(NULLIF(TRIM(CAST(p.slot AS varchar)), '')) AS has_slot,
    COUNT(NULLIF(TRIM(CAST(p.serial_no AS varchar)), '')) AS has_serial,
    COUNT(NULLIF(TRIM(CAST(pm.name AS varchar)), '')) AS has_model,
    COUNT(NULLIF(TRIM(CAST(v.name AS varchar)), '')) AS has_vendor,
    COUNT(NULLIF(TRIM(CAST(pm.cores AS varchar)), '')) AS has_cores,
    COUNT(NULLIF(TRIM(CAST(pm.speed AS varchar)), '')) AS has_speed,
    COUNT(NULLIF(TRIM(CAST(pm.speed_unit AS varchar)), '')) AS has_speed_unit,
    COUNT(NULLIF(TRIM(CAST(p.first_added AS varchar)), '')) AS has_first_added,
    COUNT(NULLIF(TRIM(CAST(p.last_updated AS varchar)), '')) AS has_last_updated
FROM view_part_v1 p JOIN view_partmodel_v1 pm ON pm.partmodel_pk=p.partmodel_fk LEFT JOIN view_vendor_v1 v ON v.vendor_pk=pm.vendor_fk
WHERE pm.type_name='CPU' AND EXISTS (SELECT 1 FROM c WHERE c.device_pk=p.device_fk);

-- name: ram-module-fields
WITH c AS (
    SELECT * FROM view_device_v2
    WHERE type IN ('physical', 'virtual')
      AND (network_device = false OR network_device IS NULL)
      AND (virtualsubtype IS NULL OR virtualsubtype <> 'Docker Container')
      AND (physicalsubtype IS NULL OR physicalsubtype NOT IN
          ('Network Printer', 'PDU', 'CRAC', 'UPS', 'Branch Circuit Power Meter', 'Power Unit', 'Environment Monitor'))
)
SELECT COUNT(*) AS rows,
    COUNT(NULLIF(TRIM(CAST(p.slot AS varchar)), '')) AS has_slot,
    COUNT(NULLIF(TRIM(CAST(p.serial_no AS varchar)), '')) AS has_serial,
    COUNT(NULLIF(TRIM(CAST(pm.name AS varchar)), '')) AS has_model,
    COUNT(NULLIF(TRIM(CAST(v.name AS varchar)), '')) AS has_vendor,
    COUNT(NULLIF(TRIM(CAST(pm.ramsize AS varchar)), '')) AS has_capacity,
    COUNT(NULLIF(TRIM(CAST(pm.ramsize_unit AS varchar)), '')) AS has_capacity_unit,
    COUNT(NULLIF(TRIM(CAST(pm.ramtype AS varchar)), '')) AS has_ram_type,
    COUNT(NULLIF(TRIM(CAST(p.first_added AS varchar)), '')) AS has_first_added,
    COUNT(NULLIF(TRIM(CAST(p.last_updated AS varchar)), '')) AS has_last_updated
FROM view_part_v1 p JOIN view_partmodel_v1 pm ON pm.partmodel_pk=p.partmodel_fk LEFT JOIN view_vendor_v1 v ON v.vendor_pk=pm.vendor_fk
WHERE pm.type_name='RAM' AND EXISTS (SELECT 1 FROM c WHERE c.device_pk=p.device_fk);

-- name: disk-fields
WITH c AS (
    SELECT * FROM view_device_v2
    WHERE type IN ('physical', 'virtual')
      AND (network_device = false OR network_device IS NULL)
      AND (virtualsubtype IS NULL OR virtualsubtype <> 'Docker Container')
      AND (physicalsubtype IS NULL OR physicalsubtype NOT IN
          ('Network Printer', 'PDU', 'CRAC', 'UPS', 'Branch Circuit Power Meter', 'Power Unit', 'Environment Monitor'))
)
SELECT COUNT(*) AS rows,
    COUNT(NULLIF(TRIM(CAST(p.slot AS varchar)), '')) AS has_slot,
    COUNT(NULLIF(TRIM(CAST(p.serial_no AS varchar)), '')) AS has_serial,
    COUNT(NULLIF(TRIM(CAST(pm.name AS varchar)), '')) AS has_model,
    COUNT(NULLIF(TRIM(CAST(v.name AS varchar)), '')) AS has_vendor,
    COUNT(NULLIF(TRIM(CAST(pm.hdsize AS varchar)), '')) AS has_capacity,
    COUNT(NULLIF(TRIM(CAST(pm.hdsize_unit AS varchar)), '')) AS has_capacity_unit,
    COUNT(NULLIF(TRIM(CAST(pm.hddtype_name AS varchar)), '')) AS has_disk_type,
    COUNT(NULLIF(TRIM(CAST(pm.media_type_name AS varchar)), '')) AS has_media_type,
    COUNT(NULLIF(TRIM(CAST(p.first_added AS varchar)), '')) AS has_first_added,
    COUNT(NULLIF(TRIM(CAST(p.last_updated AS varchar)), '')) AS has_last_updated
FROM view_part_v1 p JOIN view_partmodel_v1 pm ON pm.partmodel_pk=p.partmodel_fk LEFT JOIN view_vendor_v1 v ON v.vendor_pk=pm.vendor_fk
WHERE pm.type_name='Hard Disk' AND EXISTS (SELECT 1 FROM c WHERE c.device_pk=p.device_fk);

-- name: gpu-fields
WITH c AS (
    SELECT * FROM view_device_v2
    WHERE type IN ('physical', 'virtual')
      AND (network_device = false OR network_device IS NULL)
      AND (virtualsubtype IS NULL OR virtualsubtype <> 'Docker Container')
      AND (physicalsubtype IS NULL OR physicalsubtype NOT IN
          ('Network Printer', 'PDU', 'CRAC', 'UPS', 'Branch Circuit Power Meter', 'Power Unit', 'Environment Monitor'))
)
SELECT COUNT(*) AS rows,
    COUNT(NULLIF(TRIM(CAST(p.slot AS varchar)), '')) AS has_slot,
    COUNT(NULLIF(TRIM(CAST(p.serial_no AS varchar)), '')) AS has_serial,
    COUNT(NULLIF(TRIM(CAST(pm.name AS varchar)), '')) AS has_model,
    COUNT(NULLIF(TRIM(CAST(v.name AS varchar)), '')) AS has_vendor,
    COUNT(NULLIF(TRIM(CAST(pm.ramsize AS varchar)), '')) AS has_memory,
    COUNT(NULLIF(TRIM(CAST(pm.ramsize_unit AS varchar)), '')) AS has_memory_unit,
    COUNT(NULLIF(TRIM(CAST(p.first_added AS varchar)), '')) AS has_first_added,
    COUNT(NULLIF(TRIM(CAST(p.last_updated AS varchar)), '')) AS has_last_updated
FROM view_part_v1 p JOIN view_partmodel_v1 pm ON pm.partmodel_pk=p.partmodel_fk LEFT JOIN view_vendor_v1 v ON v.vendor_pk=pm.vendor_fk
WHERE pm.type_name='GPU' AND EXISTS (SELECT 1 FROM c WHERE c.device_pk=p.device_fk);

-- name: os-fields
WITH c AS (
    SELECT * FROM view_device_v2
    WHERE type IN ('physical', 'virtual')
      AND (network_device = false OR network_device IS NULL)
      AND (virtualsubtype IS NULL OR virtualsubtype <> 'Docker Container')
      AND (physicalsubtype IS NULL OR physicalsubtype NOT IN
          ('Network Printer', 'PDU', 'CRAC', 'UPS', 'Branch Circuit Power Meter', 'Power Unit', 'Environment Monitor'))
)
SELECT COUNT(*) AS rows,
    COUNT(NULLIF(TRIM(CAST(o.os_name AS varchar)), '')) AS has_name,
    COUNT(NULLIF(TRIM(CAST(o.os_version AS varchar)), '')) AS has_version,
    COUNT(NULLIF(TRIM(CAST(o.os_version_no AS varchar)), '')) AS has_build,
    COUNT(NULLIF(TRIM(CAST(v.name AS varchar)), '')) AS has_vendor
FROM view_deviceos_v1 o LEFT JOIN view_os_v1 m ON m.os_pk=o.os_fk LEFT JOIN view_vendor_v1 v ON v.vendor_pk=m.vendor_fk
WHERE EXISTS (SELECT 1 FROM c WHERE c.device_pk=o.device_fk);

-- name: filesystem-fields
WITH c AS (
    SELECT * FROM view_device_v2
    WHERE type IN ('physical', 'virtual')
      AND (network_device = false OR network_device IS NULL)
      AND (virtualsubtype IS NULL OR virtualsubtype <> 'Docker Container')
      AND (physicalsubtype IS NULL OR physicalsubtype NOT IN
          ('Network Printer', 'PDU', 'CRAC', 'UPS', 'Branch Circuit Power Meter', 'Power Unit', 'Environment Monitor'))
)
SELECT COUNT(*) AS rows,
    COUNT(NULLIF(TRIM(CAST(m.mountpoint AS varchar)), '')) AS has_mount,
    COUNT(NULLIF(TRIM(CAST(m.filesystem AS varchar)), '')) AS has_filesystem,
    COUNT(NULLIF(TRIM(CAST(m.fstype_name AS varchar)), '')) AS has_fstype,
    COUNT(NULLIF(TRIM(CAST(m.capacity AS varchar)), '')) AS has_capacity,
    COUNT(NULLIF(TRIM(CAST(m.free_capacity AS varchar)), '')) AS has_free_capacity,
    COUNT(NULLIF(TRIM(CAST(m.label AS varchar)), '')) AS has_label,
    COUNT(NULLIF(TRIM(CAST(m.first_added AS varchar)), '')) AS has_first_added,
    COUNT(NULLIF(TRIM(CAST(m.last_updated AS varchar)), '')) AS has_last_updated
FROM view_mountpoint_v2 m
WHERE EXISTS (SELECT 1 FROM c WHERE c.device_pk=ANY(m.device_fks));

-- name: interface-fields
WITH c AS (
    SELECT * FROM view_device_v2
    WHERE type IN ('physical', 'virtual')
      AND (network_device = false OR network_device IS NULL)
      AND (virtualsubtype IS NULL OR virtualsubtype <> 'Docker Container')
      AND (physicalsubtype IS NULL OR physicalsubtype NOT IN
          ('Network Printer', 'PDU', 'CRAC', 'UPS', 'Branch Circuit Power Meter', 'Power Unit', 'Environment Monitor'))
)
SELECT COUNT(*) AS rows,
    COUNT(NULLIF(TRIM(CAST(n.port AS varchar)), '')) AS has_port,
    COUNT(NULLIF(TRIM(CAST(n.hwaddress AS varchar)), '')) AS has_mac,
    COUNT(NULLIF(TRIM(CAST(n.hwaddress2 AS varchar)), '')) AS has_mac2,
    COUNT(NULLIF(TRIM(CAST(n.port_speed AS varchar)), '')) AS has_speed,
    COUNT(NULLIF(TRIM(CAST(n.global_type AS varchar)), '')) AS has_global_type,
    COUNT(NULLIF(TRIM(CAST(n.type_name AS varchar)), '')) AS has_type_name,
    COUNT(NULLIF(TRIM(CAST(n.mtu AS varchar)), '')) AS has_mtu,
    COUNT(NULLIF(TRIM(CAST(n.physical_state AS varchar)), '')) AS has_physical_state,
    COUNT(NULLIF(TRIM(CAST(n.part_fk AS varchar)), '')) AS has_part_fk,
    COUNT(NULLIF(TRIM(CAST(v.name AS varchar)), '')) AS has_vendor,
    COUNT(NULLIF(TRIM(CAST(n.first_added AS varchar)), '')) AS has_first_added,
    COUNT(NULLIF(TRIM(CAST(n.last_edited AS varchar)), '')) AS has_last_edited
FROM view_netport_v1 n LEFT JOIN view_vendor_v1 v ON v.vendor_pk=n.vendor_fk
WHERE EXISTS (SELECT 1 FROM c WHERE c.device_pk=n.device_fk);

-- name: ip-fields
WITH c AS (
    SELECT * FROM view_device_v2
    WHERE type IN ('physical', 'virtual')
      AND (network_device = false OR network_device IS NULL)
      AND (virtualsubtype IS NULL OR virtualsubtype <> 'Docker Container')
      AND (physicalsubtype IS NULL OR physicalsubtype NOT IN
          ('Network Printer', 'PDU', 'CRAC', 'UPS', 'Branch Circuit Power Meter', 'Power Unit', 'Environment Monitor'))
)
SELECT COUNT(*) AS rows,
    COUNT(NULLIF(TRIM(CAST(i.ip_address AS varchar)), '')) AS has_address,
    COUNT(NULLIF(TRIM(CAST(i.netport_fk AS varchar)), '')) AS has_netport_fk,
    COUNT(NULLIF(TRIM(CAST(i.subnet_fk AS varchar)), '')) AS has_subnet_fk,
    COUNT(NULLIF(TRIM(CAST(b.mask_bits AS varchar)), '')) AS has_prefix,
    COUNT(NULLIF(TRIM(CAST(b.gateway AS varchar)), '')) AS has_gateway,
    COUNT(NULLIF(TRIM(CAST(i.is_shared AS varchar)), '')) AS has_is_shared
FROM view_ipaddress_v2 i LEFT JOIN view_subnet_v1 b ON b.subnet_pk=i.subnet_fk
WHERE EXISTS (SELECT 1 FROM c WHERE c.device_pk=ANY(i.device_fks));

-- name: software-fields
WITH c AS (
    SELECT * FROM view_device_v2
    WHERE type IN ('physical', 'virtual')
      AND (network_device = false OR network_device IS NULL)
      AND (virtualsubtype IS NULL OR virtualsubtype <> 'Docker Container')
      AND (physicalsubtype IS NULL OR physicalsubtype NOT IN
          ('Network Printer', 'PDU', 'CRAC', 'UPS', 'Branch Circuit Power Meter', 'Power Unit', 'Environment Monitor'))
)
SELECT COUNT(*) AS rows,
    COUNT(NULLIF(TRIM(CAST(s.name AS varchar)), '')) AS has_name,
    COUNT(NULLIF(TRIM(CAST(u.version AS varchar)), '')) AS has_version,
    COUNT(NULLIF(TRIM(CAST(u.install_path AS varchar)), '')) AS has_install_path,
    COUNT(NULLIF(TRIM(CAST(u.install_date AS varchar)), '')) AS has_install_date,
    COUNT(NULLIF(TRIM(CAST(u.first_detected AS varchar)), '')) AS has_first_detected,
    COUNT(NULLIF(TRIM(CAST(u.last_updated AS varchar)), '')) AS has_last_updated,
    COUNT(NULLIF(TRIM(CAST(v.name AS varchar)), '')) AS has_vendor
FROM view_softwareinuse_v1 u LEFT JOIN view_software_v1 s ON s.software_pk=u.software_fk LEFT JOIN view_vendor_v1 v ON v.vendor_pk=s.vendor_fk
WHERE EXISTS (SELECT 1 FROM c WHERE c.device_pk=u.device_fk);

-- name: related-coverage
WITH c AS (
    SELECT * FROM view_device_v2
    WHERE type IN ('physical', 'virtual')
      AND (network_device = false OR network_device IS NULL)
      AND (virtualsubtype IS NULL OR virtualsubtype <> 'Docker Container')
      AND (physicalsubtype IS NULL OR physicalsubtype NOT IN
          ('Network Printer', 'PDU', 'CRAC', 'UPS', 'Branch Circuit Power Meter', 'Power Unit', 'Environment Monitor'))
)
SELECT 'cpu' AS category, COUNT(DISTINCT x.part_pk) AS source_rows, COUNT(DISTINCT c.device_pk) AS devices, COUNT(*) AS relation_pairs
FROM view_part_v1 x JOIN view_partmodel_v1 m ON m.partmodel_pk=x.partmodel_fk JOIN c ON c.device_pk=x.device_fk WHERE m.type_name='CPU'
UNION ALL
SELECT 'ram-module' AS category, COUNT(DISTINCT x.part_pk) AS source_rows, COUNT(DISTINCT c.device_pk) AS devices, COUNT(*) AS relation_pairs
FROM view_part_v1 x JOIN view_partmodel_v1 m ON m.partmodel_pk=x.partmodel_fk JOIN c ON c.device_pk=x.device_fk WHERE m.type_name='RAM'
UNION ALL
SELECT 'disk' AS category, COUNT(DISTINCT x.part_pk) AS source_rows, COUNT(DISTINCT c.device_pk) AS devices, COUNT(*) AS relation_pairs
FROM view_part_v1 x JOIN view_partmodel_v1 m ON m.partmodel_pk=x.partmodel_fk JOIN c ON c.device_pk=x.device_fk WHERE m.type_name='Hard Disk'
UNION ALL
SELECT 'gpu' AS category, COUNT(DISTINCT x.part_pk) AS source_rows, COUNT(DISTINCT c.device_pk) AS devices, COUNT(*) AS relation_pairs
FROM view_part_v1 x JOIN view_partmodel_v1 m ON m.partmodel_pk=x.partmodel_fk JOIN c ON c.device_pk=x.device_fk WHERE m.type_name='GPU'
UNION ALL
SELECT 'os' AS category, COUNT(DISTINCT x.deviceos_pk) AS source_rows, COUNT(DISTINCT c.device_pk) AS devices, COUNT(*) AS relation_pairs
FROM view_deviceos_v1 x JOIN c ON c.device_pk=x.device_fk
UNION ALL
SELECT 'filesystem' AS category, COUNT(DISTINCT x.mountpoint_pk) AS source_rows, COUNT(DISTINCT c.device_pk) AS devices, COUNT(*) AS relation_pairs
FROM view_mountpoint_v2 x JOIN c ON c.device_pk=ANY(x.device_fks)
UNION ALL
SELECT 'interface' AS category, COUNT(DISTINCT x.netport_pk) AS source_rows, COUNT(DISTINCT c.device_pk) AS devices, COUNT(*) AS relation_pairs
FROM view_netport_v1 x JOIN c ON c.device_pk=x.device_fk
UNION ALL
SELECT 'ip' AS category, COUNT(DISTINCT x.ipaddress_pk) AS source_rows, COUNT(DISTINCT c.device_pk) AS devices, COUNT(*) AS relation_pairs
FROM view_ipaddress_v2 x JOIN c ON c.device_pk=ANY(x.device_fks)
UNION ALL
SELECT 'software' AS category, COUNT(DISTINCT x.softwareinuse_pk) AS source_rows, COUNT(DISTINCT c.device_pk) AS devices, COUNT(*) AS relation_pairs
FROM view_softwareinuse_v1 x JOIN c ON c.device_pk=x.device_fk;

-- name: host-links
WITH c AS (
    SELECT * FROM view_device_v2
    WHERE type IN ('physical', 'virtual')
      AND (network_device = false OR network_device IS NULL)
      AND (virtualsubtype IS NULL OR virtualsubtype <> 'Docker Container')
      AND (physicalsubtype IS NULL OR physicalsubtype NOT IN
          ('Network Printer', 'PDU', 'CRAC', 'UPS', 'Branch Circuit Power Meter', 'Power Unit', 'Environment Monitor'))
)
SELECT COUNT(*) AS computers,
    COUNT(c.virtual_host_device_fk) AS has_host_fk,
    COUNT(h.device_pk) AS matched_host,
    COUNT(t.device_pk) AS host_in_sample,
    COUNT(c.host_chassis_device_fk) AS has_chassis_fk,
    COUNT(ch.device_pk) AS matched_chassis,
    COUNT(c.vm_manager_device_fk) AS has_manager_fk,
    COUNT(m.device_pk) AS matched_manager
FROM c
LEFT JOIN view_device_v2 h ON h.device_pk=c.virtual_host_device_fk
LEFT JOIN c t ON t.device_pk=c.virtual_host_device_fk
LEFT JOIN view_device_v2 ch ON ch.device_pk=c.host_chassis_device_fk
LEFT JOIN view_device_v2 m ON m.device_pk=c.vm_manager_device_fk;

-- name: units-and-filesystems
WITH c AS (
    SELECT * FROM view_device_v2
    WHERE type IN ('physical', 'virtual')
      AND (network_device = false OR network_device IS NULL)
      AND (virtualsubtype IS NULL OR virtualsubtype <> 'Docker Container')
      AND (physicalsubtype IS NULL OR physicalsubtype NOT IN
          ('Network Printer', 'PDU', 'CRAC', 'UPS', 'Branch Circuit Power Meter', 'Power Unit', 'Environment Monitor'))
)
SELECT 'ram_unit' AS field, CAST(d.ram_size_type AS varchar) AS value, COUNT(*) AS rows FROM c d GROUP BY d.ram_size_type
UNION ALL
SELECT 'part_type', pm.type_name, COUNT(*) FROM view_part_v1 p JOIN view_partmodel_v1 pm ON pm.partmodel_pk=p.partmodel_fk JOIN c ON c.device_pk=p.device_fk GROUP BY pm.type_name
UNION ALL
SELECT 'ram_module_unit', pm.ramsize_unit, COUNT(*) FROM view_part_v1 p JOIN view_partmodel_v1 pm ON pm.partmodel_pk=p.partmodel_fk JOIN c ON c.device_pk=p.device_fk WHERE pm.type_name='RAM' GROUP BY pm.ramsize_unit
UNION ALL
SELECT 'cpu_speed_unit', pm.speed_unit, COUNT(*) FROM view_part_v1 p JOIN view_partmodel_v1 pm ON pm.partmodel_pk=p.partmodel_fk JOIN c ON c.device_pk=p.device_fk WHERE pm.type_name='CPU' GROUP BY pm.speed_unit
UNION ALL
SELECT 'filesystem_type', m.fstype_name, COUNT(*) FROM view_mountpoint_v2 m WHERE EXISTS (SELECT 1 FROM c WHERE c.device_pk=ANY(m.device_fks)) GROUP BY m.fstype_name;

-- name: ip-port-links
WITH c AS (
    SELECT * FROM view_device_v2
    WHERE type IN ('physical', 'virtual')
      AND (network_device = false OR network_device IS NULL)
      AND (virtualsubtype IS NULL OR virtualsubtype <> 'Docker Container')
      AND (physicalsubtype IS NULL OR physicalsubtype NOT IN
          ('Network Printer', 'PDU', 'CRAC', 'UPS', 'Branch Circuit Power Meter', 'Power Unit', 'Environment Monitor'))
)
SELECT COUNT(*) AS ip_rows, COUNT(i.netport_fk) AS has_port_fk,
    COUNT(n.netport_pk) AS matched_port,
    SUM(CASE WHEN n.device_fk=ANY(i.device_fks) THEN 1 ELSE 0 END) AS port_device_in_ip_devices,
    COUNT(b.subnet_pk) AS matched_subnet
FROM view_ipaddress_v2 i
LEFT JOIN view_netport_v1 n ON n.netport_pk=i.netport_fk
LEFT JOIN view_subnet_v1 b ON b.subnet_pk=i.subnet_fk
WHERE EXISTS (SELECT 1 FROM c WHERE c.device_pk=ANY(i.device_fks));

-- name: ram-layout
WITH c AS (
    SELECT * FROM view_device_v2
    WHERE type IN ('physical', 'virtual')
      AND (network_device = false OR network_device IS NULL)
      AND (virtualsubtype IS NULL OR virtualsubtype <> 'Docker Container')
      AND (physicalsubtype IS NULL OR physicalsubtype NOT IN
          ('Network Printer', 'PDU', 'CRAC', 'UPS', 'Branch Circuit Power Meter', 'Power Unit', 'Environment Monitor'))
), layouts AS (
    SELECT p.device_fk, COUNT(*) AS modules, COUNT(DISTINCT NULLIF(TRIM(p.slot), '')) AS distinct_slots
    FROM view_part_v1 p JOIN view_partmodel_v1 pm ON pm.partmodel_pk=p.partmodel_fk
    JOIN c ON c.device_pk=p.device_fk WHERE pm.type_name='RAM' GROUP BY p.device_fk
)
SELECT modules, distinct_slots, COUNT(*) AS devices FROM layouts GROUP BY modules, distinct_slots ORDER BY modules, distinct_slots;
