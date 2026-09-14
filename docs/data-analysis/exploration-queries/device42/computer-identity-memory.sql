-- Computer 원천의 식별 후보·RAM 집계 검증. source PK의 장기 안정성을 보증하지 않는다.
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

-- name: ram-total-comparison
WITH c AS (
    SELECT * FROM view_device_v2
    WHERE type IN ('physical', 'virtual')
      AND (network_device = false OR network_device IS NULL)
      AND (virtualsubtype IS NULL OR virtualsubtype <> 'Docker Container')
      AND (physicalsubtype IS NULL OR physicalsubtype NOT IN
          ('Network Printer', 'PDU', 'CRAC', 'UPS', 'Branch Circuit Power Meter', 'Power Unit', 'Environment Monitor'))
), sums AS (
    SELECT p.device_fk, COUNT(*) AS modules, COUNT(pm.ramsize) AS has_size,
        COUNT(NULLIF(TRIM(pm.ramsize_unit), '')) AS has_unit,
        COUNT(DISTINCT pm.ramsize_unit) AS unit_count,
        MIN(pm.ramsize_unit) AS unit, SUM(pm.ramsize) AS module_total
    FROM view_part_v1 p JOIN view_partmodel_v1 pm ON pm.partmodel_pk=p.partmodel_fk
    JOIN c ON c.device_pk=p.device_fk WHERE pm.type_name='RAM' GROUP BY p.device_fk
)
SELECT COUNT(*) AS devices_with_modules,
    SUM(CASE WHEN s.modules=s.has_size AND s.modules=s.has_unit AND s.unit_count=1 AND s.unit=c.ram_size_type AND c.ram IS NOT NULL THEN 1 ELSE 0 END) AS comparable,
    SUM(CASE WHEN s.modules=s.has_size AND s.modules=s.has_unit AND s.unit_count=1 AND s.unit=c.ram_size_type AND s.module_total=c.ram THEN 1 ELSE 0 END) AS totals_equal
FROM sums s JOIN c ON c.device_pk=s.device_fk;

-- name: identity-duplicates
WITH c AS (
    SELECT * FROM view_device_v2
    WHERE type IN ('physical', 'virtual')
      AND (network_device = false OR network_device IS NULL)
      AND (virtualsubtype IS NULL OR virtualsubtype <> 'Docker Container')
      AND (physicalsubtype IS NULL OR physicalsubtype NOT IN
          ('Network Printer', 'PDU', 'CRAC', 'UPS', 'Branch Circuit Power Meter', 'Power Unit', 'Environment Monitor'))
), names AS (
    SELECT name, COUNT(*) AS n FROM c WHERE NULLIF(TRIM(name), '') IS NOT NULL GROUP BY name HAVING COUNT(*)>1
), uuids AS (
    SELECT uuid, COUNT(*) AS n FROM c WHERE NULLIF(TRIM(uuid), '') IS NOT NULL GROUP BY uuid HAVING COUNT(*)>1
), serials AS (
    SELECT serial_no, COUNT(*) AS n FROM c WHERE NULLIF(TRIM(serial_no), '') IS NOT NULL GROUP BY serial_no HAVING COUNT(*)>1
)
SELECT 'name' AS field, COUNT(*) AS duplicate_values FROM names
UNION ALL SELECT 'uuid', COUNT(*) FROM uuids
UNION ALL SELECT 'serial_no', COUNT(*) FROM serials;

-- name: part-quantity
WITH c AS (
    SELECT * FROM view_device_v2
    WHERE type IN ('physical', 'virtual')
      AND (network_device = false OR network_device IS NULL)
      AND (virtualsubtype IS NULL OR virtualsubtype <> 'Docker Container')
      AND (physicalsubtype IS NULL OR physicalsubtype NOT IN
          ('Network Printer', 'PDU', 'CRAC', 'UPS', 'Branch Circuit Power Meter', 'Power Unit', 'Environment Monitor'))
)
SELECT pm.type_name, p.pcount, COUNT(*) AS part_rows
FROM view_part_v1 p JOIN view_partmodel_v1 pm ON pm.partmodel_pk=p.partmodel_fk
JOIN c ON c.device_pk=p.device_fk
WHERE pm.type_name IN ('CPU', 'RAM', 'Hard Disk', 'GPU')
GROUP BY pm.type_name, p.pcount ORDER BY pm.type_name, p.pcount;
