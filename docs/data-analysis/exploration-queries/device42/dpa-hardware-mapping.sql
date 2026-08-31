-- DPALOGICALDRIVE, DPANETADAPTER, DPAMEDIAADAPTER 매핑 원천과 값 분포를 확인한다.
-- DEPLOYEDASSET의 적재 조건과 COMPUTER 분류 조건을 함께 적용한다.

-- name: logical-drive-source
SELECT
    d.device_pk AS device_fk,
    m.mountpoint,
    m.filesystem,
    m.fstype_name,
    m.capacity,
    m.free_capacity,
    m.label,
    m.first_added,
    m.last_updated
FROM view_mountpoint_v2 m
JOIN view_device_v2 d ON d.device_pk = ANY(m.device_fks)
WHERE d.type IN ('virtual', 'physical')
  AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
  AND (d.network_device = false OR d.network_device IS NULL)
  AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'Network Printer')
  AND LOWER(COALESCE(m.fstype_name, '')) NOT IN ('overlay', 'devtmpfs', 'efivarfs')
ORDER BY m.mountpoint_pk, d.device_pk;

-- name: logical-drive-filesystems
SELECT
    COALESCE(m.fstype_name, '<NULL>') AS fstype_name,
    count(*) AS row_count
FROM view_mountpoint_v2 m
JOIN view_device_v2 d ON d.device_pk = ANY(m.device_fks)
WHERE d.type IN ('virtual', 'physical')
  AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
  AND (d.network_device = false OR d.network_device IS NULL)
  AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'Network Printer')
GROUP BY COALESCE(m.fstype_name, '<NULL>')
ORDER BY row_count DESC, fstype_name;

-- name: net-adapter-source
SELECT
    n.device_fk,
    n.port,
    n.description,
    n.type_name,
    n.discovered_type,
    n.normalized_port,
    n.hwaddress,
    n.port_type,
    n.name,
    n.port_speed,
    n.speedcapable,
    n.vendor_fk,
    n.txtype,
    n.physical_state,
    n.fctype,
    n.mtu,
    n.auto_neg_mode,
    n.global_type,
    n.hwaddress2,
    n.part_fk,
    n.first_added,
    n.last_edited
FROM view_netport_v1 n
JOIN view_device_v2 d ON d.device_pk = n.device_fk
WHERE d.type IN ('virtual', 'physical')
  AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
  AND (d.network_device = false OR d.network_device IS NULL)
  AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'Network Printer')
ORDER BY n.device_fk, n.netport_pk;

-- name: net-adapter-values
SELECT
    COALESCE(n.port_type, '<NULL>') AS port_type,
    COALESCE(n.type_name, '<NULL>') AS type_name,
    COALESCE(n.discovered_type, '<NULL>') AS discovered_type,
    COALESCE(n.global_type, '<NULL>') AS global_type,
    COALESCE(n.txtype, '<NULL>') AS txtype,
    COALESCE(CAST(n.port_speed AS varchar), '<NULL>') AS port_speed,
    COALESCE(n.speedcapable, '<NULL>') AS speedcapable,
    count(*) AS row_count
FROM view_netport_v1 n
JOIN view_device_v2 d ON d.device_pk = n.device_fk
WHERE d.type IN ('virtual', 'physical')
  AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
  AND (d.network_device = false OR d.network_device IS NULL)
  AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'Network Printer')
GROUP BY
    COALESCE(n.port_type, '<NULL>'),
    COALESCE(n.type_name, '<NULL>'),
    COALESCE(n.discovered_type, '<NULL>'),
    COALESCE(n.global_type, '<NULL>'),
    COALESCE(n.txtype, '<NULL>'),
    COALESCE(CAST(n.port_speed AS varchar), '<NULL>'),
    COALESCE(n.speedcapable, '<NULL>')
ORDER BY row_count DESC;

-- name: media-adapter-source
SELECT
    p.device_fk,
    p.asset_no,
    p.serial_no,
    p.description,
    p.firmware,
    p.slot,
    pm.name AS model_name,
    pm.description AS model_description,
    pm.ramsize,
    pm.ramsize_unit,
    pm.ramtype,
    pm.connectivity_name,
    pm.media_type_name,
    pm.connector_type_name,
    v.name AS vendor_name,
    p.first_added,
    p.last_updated
FROM view_part_v1 p
JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
JOIN view_device_v2 d ON d.device_pk = p.device_fk
LEFT JOIN view_vendor_v1 v ON v.vendor_pk = pm.vendor_fk
WHERE pm.type_name = 'GPU'
  AND d.type IN ('virtual', 'physical')
  AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
  AND (d.network_device = false OR d.network_device IS NULL)
  AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'Network Printer')
ORDER BY p.device_fk, p.part_pk;
