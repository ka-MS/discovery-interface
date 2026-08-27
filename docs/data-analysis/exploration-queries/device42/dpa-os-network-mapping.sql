-- DPAOS / DPATCPIP / DPANETPRINTER 의 COMPUTER·NETPRINTER 대상 원천과 값 분포를
-- 확인한다. 대상 판정 조건은 DeployedAssetIntegrate 의 DEVICE_FILTER 와
-- 자식 ASSETCLASS 조건을 그대로 쓴다.
--
-- 라이센스 키 계열 컬럼은 비밀값이 될 수 있어 값을 뽑지 않고 충전 건수만 센다.

-- device 당 deviceos 건수 분포. DPAOS 가 1:N 인지 확인한다.
-- name: os-cardinality
WITH target AS (
    SELECT d.device_pk
    FROM view_device_v2 d
    WHERE d.type IN ('virtual', 'physical')
      AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
      AND (d.network_device = false OR d.network_device IS NULL)
      AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'Network Printer')
)
SELECT os_cnt, COUNT(*) AS device_cnt
FROM (
    SELECT t.device_pk, COUNT(o.deviceos_pk) AS os_cnt
    FROM target t
    LEFT JOIN view_deviceos_v1 o ON o.device_fk = t.device_pk
    GROUP BY t.device_pk
) s
GROUP BY os_cnt
ORDER BY os_cnt;

-- DPAOS 후보 컬럼별 충전 건수와 최대 길이.
-- name: os-coverage
WITH src AS (
    SELECT o.device_fk, o.os_name, o.os_version, o.os_version_no, o.os_arch,
        o.os_arch_name, o.os_license_key, o.discovered_license_key,
        s.name AS os_catalog_name, v.name AS vendor_name
    FROM view_deviceos_v1 o
    JOIN view_device_v2 d ON d.device_pk = o.device_fk
    LEFT JOIN view_os_v1 s ON s.os_pk = o.os_fk
    LEFT JOIN view_vendor_v1 v ON v.vendor_pk = s.vendor_fk
    WHERE d.type IN ('virtual', 'physical')
      AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
      AND (d.network_device = false OR d.network_device IS NULL)
      AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'Network Printer')
)
SELECT 'row-total' AS col, COUNT(*) AS filled, 0 AS max_len FROM src
UNION ALL SELECT 'os_name', COUNT(NULLIF(os_name, '')), MAX(LENGTH(os_name)) FROM src
UNION ALL SELECT 'os_catalog_name', COUNT(NULLIF(os_catalog_name, '')), MAX(LENGTH(os_catalog_name)) FROM src
UNION ALL SELECT 'vendor_name', COUNT(NULLIF(vendor_name, '')), MAX(LENGTH(vendor_name)) FROM src
UNION ALL SELECT 'os_version', COUNT(NULLIF(os_version, '')), MAX(LENGTH(os_version)) FROM src
UNION ALL SELECT 'os_version_no', COUNT(NULLIF(os_version_no, '')), MAX(LENGTH(os_version_no)) FROM src
UNION ALL SELECT 'os_arch_name', COUNT(NULLIF(os_arch_name, '')), MAX(LENGTH(os_arch_name)) FROM src
UNION ALL SELECT 'os_license_key', COUNT(NULLIF(os_license_key, '')), 0 FROM src
UNION ALL SELECT 'discovered_license_key', COUNT(NULLIF(discovered_license_key, '')), 0 FROM src;

-- 값 형태 확인용 표본. 라이센스 키는 제외한다.
-- name: os-sample
SELECT o.device_fk, s.name AS os_catalog_name, v.name AS vendor_name,
    o.os_name, o.os_version, o.os_version_no, o.os_arch_name
FROM view_deviceos_v1 o
JOIN view_device_v2 d ON d.device_pk = o.device_fk
LEFT JOIN view_os_v1 s ON s.os_pk = o.os_fk
LEFT JOIN view_vendor_v1 v ON v.vendor_pk = s.vendor_fk
WHERE d.type IN ('virtual', 'physical')
  AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
  AND (d.network_device = false OR d.network_device IS NULL)
  AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'Network Printer')
ORDER BY o.device_fk
LIMIT 40;

-- device 당 IP 건수 분포. DPATCPIP 는 Maximo 에서 1:1 이라 선택 규칙이 필요하다.
-- name: tcpip-cardinality
WITH target AS (
    SELECT d.device_pk
    FROM view_device_v2 d
    WHERE d.type IN ('virtual', 'physical')
      AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
      AND (d.network_device = false OR d.network_device IS NULL)
      AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'Network Printer')
)
SELECT ip_cnt, COUNT(*) AS device_cnt
FROM (
    SELECT t.device_pk, COUNT(i.ipaddress_pk) AS ip_cnt
    FROM target t
    LEFT JOIN view_ipaddress_v1 i ON i.device_fk = t.device_pk
    GROUP BY t.device_pk
) s
GROUP BY ip_cnt
ORDER BY ip_cnt;

-- DPATCPIP 후보 컬럼별 충전 건수와 최대 길이.
-- name: tcpip-coverage
WITH src AS (
    SELECT i.ip_address, i.label, i.type, i.netport_fk, i.last_discovered,
        d.name AS device_name,
        b.gateway, b.mask_bits, b.name AS subnet_name
    FROM view_ipaddress_v1 i
    JOIN view_device_v2 d ON d.device_pk = i.device_fk
    LEFT JOIN view_subnet_v1 b ON b.subnet_pk = i.subnet_fk
    WHERE d.type IN ('virtual', 'physical')
      AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
      AND (d.network_device = false OR d.network_device IS NULL)
      AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'Network Printer')
)
SELECT 'row-total' AS col, COUNT(*) AS filled, 0 AS max_len FROM src
UNION ALL SELECT 'ip_address', COUNT(NULLIF(ip_address, '')), MAX(LENGTH(ip_address)) FROM src
UNION ALL SELECT 'device_name', COUNT(NULLIF(device_name, '')), MAX(LENGTH(device_name)) FROM src
UNION ALL SELECT 'label', COUNT(NULLIF(label, '')), MAX(LENGTH(label)) FROM src
UNION ALL SELECT 'gateway', COUNT(NULLIF(gateway, '')), MAX(LENGTH(gateway)) FROM src
UNION ALL SELECT 'mask_bits', COUNT(mask_bits), MAX(mask_bits) FROM src
UNION ALL SELECT 'subnet_name', COUNT(NULLIF(subnet_name, '')), MAX(LENGTH(subnet_name)) FROM src
UNION ALL SELECT 'netport_fk', COUNT(netport_fk), 0 FROM src
UNION ALL SELECT 'last_discovered', COUNT(last_discovered), 0 FROM src
UNION ALL SELECT 'ipv6', COUNT(NULLIF(POSITION(':' IN ip_address), 0)), 0 FROM src;

-- name: tcpip-sample
SELECT i.device_fk, d.name AS device_name, i.ip_address, i.label,
    b.name AS subnet_name, b.gateway, b.mask_bits
FROM view_ipaddress_v1 i
JOIN view_device_v2 d ON d.device_pk = i.device_fk
LEFT JOIN view_subnet_v1 b ON b.subnet_pk = i.subnet_fk
WHERE d.type IN ('virtual', 'physical')
  AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
  AND (d.network_device = false OR d.network_device IS NULL)
  AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'Network Printer')
ORDER BY i.device_fk, i.ip_address
LIMIT 60;

-- NETPRINTER 대상 장비의 DPANETPRINTER 후보 값.
-- name: printer-source
SELECT d.device_pk, d.name, d.serial_no, d.ram, d.ram_size_type,
    d.hard_disk_size, d.hard_disk_size_type, d.os_name, d.os_version,
    d.details, d.ip_addresses,
    (SELECT COUNT(*) FROM view_netport_v1 n WHERE n.device_fk = d.device_pk) AS netport_cnt,
    (SELECT COUNT(*) FROM view_ipaddress_v1 i WHERE i.device_fk = d.device_pk) AS ip_cnt,
    (SELECT COUNT(*) FROM view_part_v1 p WHERE p.device_fk = d.device_pk) AS part_cnt
FROM view_device_v2 d
WHERE d.physicalsubtype = 'Network Printer'
  AND d.type IN ('virtual', 'physical')
  AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15);

-- name: printer-net
SELECT d.device_pk, n.port, n.hwaddress, i.ip_address, b.gateway, b.mask_bits
FROM view_device_v2 d
LEFT JOIN view_netport_v1 n ON n.device_fk = d.device_pk
LEFT JOIN view_ipaddress_v1 i ON i.device_fk = d.device_pk
LEFT JOIN view_subnet_v1 b ON b.subnet_pk = i.subnet_fk
WHERE d.physicalsubtype = 'Network Printer';

-- name: printer-parts
SELECT p.device_fk, pm.name AS model_name, pm.type_name, p.description,
    p.serial_no, p.details
FROM view_part_v1 p
JOIN view_device_v2 d ON d.device_pk = p.device_fk
LEFT JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
WHERE d.physicalsubtype = 'Network Printer';
