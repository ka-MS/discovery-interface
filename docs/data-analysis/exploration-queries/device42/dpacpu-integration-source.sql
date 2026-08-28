-- DpaCpuIntegrate가 사용하는 원천 조건과 반환 컬럼을 검증한다.

-- name: cpu-source-count
SELECT COUNT(*) AS row_count
FROM view_part_v1 p
JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
JOIN view_device_v2 d ON d.device_pk = p.device_fk
LEFT JOIN view_vendor_v1 v ON v.vendor_pk = pm.vendor_fk
WHERE pm.type_name = 'CPU'
  AND d.type IN ('virtual', 'physical')
  AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
  AND (d.network_device = false OR d.network_device IS NULL)
  AND (d.physicalsubtype IS NULL OR d.physicalsubtype NOT IN ('Network Printer', 'PDU'));

-- name: cpu-source-sample
SELECT
    p.part_pk,
    p.device_fk,
    p.slot,
    p.description,
    pm.name AS model_name,
    pm.cores,
    pm.speed,
    pm.speed_unit,
    v.name AS vendor_name
FROM view_part_v1 p
JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
JOIN view_device_v2 d ON d.device_pk = p.device_fk
LEFT JOIN view_vendor_v1 v ON v.vendor_pk = pm.vendor_fk
WHERE pm.type_name = 'CPU'
  AND d.type IN ('virtual', 'physical')
  AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
  AND (d.network_device = false OR d.network_device IS NULL)
  AND (d.physicalsubtype IS NULL OR d.physicalsubtype NOT IN ('Network Printer', 'PDU'))
ORDER BY p.device_fk, p.slot, p.part_pk
LIMIT 20;

-- name: cpu-source-parents
SELECT
    p.device_fk,
    COUNT(*) AS cpu_count
FROM view_part_v1 p
JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
JOIN view_device_v2 d ON d.device_pk = p.device_fk
WHERE pm.type_name = 'CPU'
  AND d.type IN ('virtual', 'physical')
  AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
  AND (d.network_device = false OR d.network_device IS NULL)
  AND (d.physicalsubtype IS NULL OR d.physicalsubtype NOT IN ('Network Printer', 'PDU'))
GROUP BY p.device_fk
ORDER BY p.device_fk;
