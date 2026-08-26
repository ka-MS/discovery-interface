-- subtype 별로 어떤 원천이 채워져 있는지 본다. 매핑 문서의 커버리지 근거다.

-- name: coverage-by-subtype
SELECT COALESCE(d.virtualsubtype, d.physicalsubtype, d.type) AS subtype,
       count(*) AS devices,
       sum(CASE WHEN EXISTS (SELECT 1 FROM view_deviceos_v1 o WHERE o.device_fk=d.device_pk) THEN 1 ELSE 0 END) AS has_os,
       sum(CASE WHEN EXISTS (SELECT 1 FROM view_softwareinuse_v1 s WHERE s.device_fk=d.device_pk) THEN 1 ELSE 0 END) AS has_software,
       sum(CASE WHEN EXISTS (SELECT 1 FROM view_netport_v1 n WHERE n.device_fk=d.device_pk) THEN 1 ELSE 0 END) AS has_netport,
       sum(CASE WHEN EXISTS (SELECT 1 FROM view_ipaddress_v1 i WHERE i.device_fk=d.device_pk) THEN 1 ELSE 0 END) AS has_ip,
       sum(CASE WHEN EXISTS (SELECT 1 FROM view_part_v1 p WHERE p.device_fk=d.device_pk) THEN 1 ELSE 0 END) AS has_part,
       sum(CASE WHEN EXISTS (SELECT 1 FROM view_mountpoint_v1 m WHERE m.device_fk=d.device_pk) THEN 1 ELSE 0 END) AS has_mount,
       sum(CASE WHEN d.hardware_fk IS NOT NULL THEN 1 ELSE 0 END) AS has_hardware
FROM view_device_v2 d
GROUP BY COALESCE(d.virtualsubtype, d.physicalsubtype, d.type)
ORDER BY 2 DESC;

-- name: identifier-fill-rate
SELECT COALESCE(virtualsubtype, physicalsubtype, type) AS subtype,
       count(*) AS devices,
       sum(CASE WHEN uuid IS NOT NULL AND uuid <> '' THEN 1 ELSE 0 END) AS has_uuid,
       sum(CASE WHEN serial_no IS NOT NULL AND serial_no <> '' THEN 1 ELSE 0 END) AS has_serial
FROM view_device_v2
WHERE type IN ('virtual','physical')
  AND (virtualsubtype_id IS NULL OR virtualsubtype_id <> 15)
GROUP BY 1
ORDER BY 2 DESC;
