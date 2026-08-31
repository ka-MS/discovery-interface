-- Device42 한 서버의 현행 적재 대상 PK·카탈로그 키·변환 값을 출력한다.
-- Maximo post-load-audit.sql 결과와 집합으로 대조한다.

-- name: expected-rows
WITH target AS (
    SELECT d.device_pk, d.network_device, d.physicalsubtype
    FROM view_device_v2 d
    WHERE d.type IN ('virtual', 'physical')
      AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
      AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'PDU')
), computer AS (
    SELECT device_pk
    FROM target
    WHERE (network_device = false OR network_device IS NULL)
      AND (physicalsubtype IS NULL OR physicalsubtype <> 'Network Printer')
), rows AS (
    SELECT 'DEPLOYEDASSET' AS entity,
           CAST(device_pk AS VARCHAR) AS target_id,
           '' AS parent_id
    FROM target
    UNION ALL
    SELECT 'DPACOMPUTER', CAST(device_pk AS VARCHAR), CAST(device_pk AS VARCHAR)
    FROM computer
    UNION ALL
    SELECT 'DPACPU', CAST(p.part_pk AS VARCHAR), CAST(p.device_fk AS VARCHAR)
    FROM view_part_v1 p
    JOIN computer c ON c.device_pk = p.device_fk
    JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
    WHERE pm.type_name = 'CPU'
    UNION ALL
    SELECT 'DPADISK', CAST(p.part_pk AS VARCHAR), CAST(p.device_fk AS VARCHAR)
    FROM view_part_v1 p
    JOIN computer c ON c.device_pk = p.device_fk
    JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
    WHERE pm.type_name = 'Hard Disk'
    UNION ALL
    SELECT DISTINCT ON (m.mountpoint_pk)
           'DPALOGICALDRIVE', CAST(m.mountpoint_pk AS VARCHAR), CAST(c.device_pk AS VARCHAR)
    FROM view_mountpoint_v2 m
    JOIN computer c ON c.device_pk = ANY(m.device_fks)
    WHERE LOWER(COALESCE(m.fstype_name, '')) NOT IN ('overlay', 'devtmpfs', 'efivarfs')
    UNION ALL
    SELECT 'DPAMEDIAADAPTER', CAST(p.part_pk AS VARCHAR), CAST(p.device_fk AS VARCHAR)
    FROM view_part_v1 p
    JOIN computer c ON c.device_pk = p.device_fk
    JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
    WHERE pm.type_name = 'GPU'
    UNION ALL
    SELECT 'DPANETADAPTER', CAST(n.netport_pk AS VARCHAR), CAST(n.device_fk AS VARCHAR)
    FROM view_netport_v1 n
    JOIN computer c ON c.device_pk = n.device_fk
    UNION ALL
    SELECT 'DPAOS', CAST(o.deviceos_pk AS VARCHAR), CAST(o.device_fk AS VARCHAR)
    FROM view_deviceos_v1 o
    JOIN computer c ON c.device_pk = o.device_fk
    UNION ALL
    SELECT 'DPASOFTWARE', CAST(u.softwareinuse_pk AS VARCHAR), CAST(u.device_fk AS VARCHAR)
    FROM view_softwareinuse_v1 u
    JOIN computer c ON c.device_pk = u.device_fk
    UNION ALL
    SELECT DISTINCT ON (i.ipaddress_pk)
           'DPATCPIP', CAST(i.ipaddress_pk AS VARCHAR), CAST(c.device_pk AS VARCHAR)
    FROM view_ipaddress_v2 i
    JOIN computer c ON c.device_pk = ANY(i.device_fks)
    UNION ALL
    SELECT 'DPANETDEVICE', CAST(device_pk AS VARCHAR), CAST(device_pk AS VARCHAR)
    FROM target
    WHERE network_device = true
    UNION ALL
    SELECT 'DPANETPRINTER', CAST(device_pk AS VARCHAR), CAST(device_pk AS VARCHAR)
    FROM target
    WHERE physicalsubtype = 'Network Printer'
    UNION ALL
    SELECT DISTINCT
           'TLOAMSOFTWARE',
           UPPER(COALESCE(NULLIF(TRIM(s.name), ''), 'UNKNOWN')) || '|'
             || UPPER(REPLACE(COALESCE(NULLIF(TRIM(u.version), ''), 'UNKNOWN'), ' ', '')) || '|'
             || UPPER(COALESCE(NULLIF(TRIM(v.name), ''), 'UNKNOWN')),
           ''
    FROM view_softwareinuse_v1 u
    JOIN computer c ON c.device_pk = u.device_fk
    LEFT JOIN view_software_v1 s ON s.software_pk = u.software_fk
    LEFT JOIN view_vendor_v1 v ON v.vendor_pk = s.vendor_fk
)
SELECT entity, target_id, parent_id
FROM rows
ORDER BY entity, target_id, parent_id;

-- name: expected-conversion-values
WITH target AS (
    SELECT d.device_pk, d.hardware_fk
    FROM view_device_v2 d
    WHERE d.type IN ('virtual', 'physical')
      AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
      AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'PDU')
), computer AS (
    SELECT d.device_pk
    FROM view_device_v2 d
    WHERE d.type IN ('virtual', 'physical')
      AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
      AND (d.network_device = false OR d.network_device IS NULL)
      AND (d.physicalsubtype IS NULL OR d.physicalsubtype NOT IN ('Network Printer', 'PDU'))
), values AS (
    SELECT 'MANUFACTURER' AS domain, v.name AS value
    FROM target t
    JOIN view_hardware_v2 h ON h.hardware_pk = t.hardware_fk
    JOIN view_vendor_v1 v ON v.vendor_pk = h.vendor_fk
    UNION
    SELECT 'MANUFACTURER', v.name
    FROM view_part_v1 p
    JOIN computer c ON c.device_pk = p.device_fk
    JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
    JOIN view_vendor_v1 v ON v.vendor_pk = pm.vendor_fk
    UNION
    SELECT 'MANUFACTURER', v.name
    FROM view_deviceos_v1 o
    JOIN computer c ON c.device_pk = o.device_fk
    JOIN view_os_v1 s ON s.os_pk = o.os_fk
    JOIN view_vendor_v1 v ON v.vendor_pk = s.vendor_fk
    UNION
    SELECT 'MANUFACTURER', v.name
    FROM view_netport_v1 n
    JOIN computer c ON c.device_pk = n.device_fk
    JOIN view_vendor_v1 v ON v.vendor_pk = n.vendor_fk
    UNION
    SELECT 'MANUFACTURER', v.name
    FROM view_softwareinuse_v1 u
    JOIN computer c ON c.device_pk = u.device_fk
    JOIN view_software_v1 sw ON sw.software_pk = u.software_fk
    JOIN view_vendor_v1 v ON v.vendor_pk = sw.vendor_fk
    UNION
    SELECT 'MANUFACTURER', 'UNKNOWN'
    UNION
    SELECT 'OS', o.os_name
    FROM view_deviceos_v1 o
    JOIN computer c ON c.device_pk = o.device_fk
    WHERE o.os_name IS NOT NULL AND o.os_name <> ''
    UNION
    SELECT 'PROCESSOR', pm.name
    FROM view_part_v1 p
    JOIN computer c ON c.device_pk = p.device_fk
    JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
    WHERE pm.type_name = 'CPU' AND pm.name IS NOT NULL AND pm.name <> ''
    UNION
    SELECT 'ADAPTER', pm.name
    FROM view_part_v1 p
    JOIN computer c ON c.device_pk = p.device_fk
    JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
    WHERE pm.type_name = 'GPU' AND pm.name IS NOT NULL AND pm.name <> ''
    UNION
    SELECT 'ADAPTER', 'UNKNOWN'
)
SELECT domain, value
FROM values
WHERE value IS NOT NULL AND value <> ''
ORDER BY domain, value;
