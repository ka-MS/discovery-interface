-- Computer 매핑의 단위·BIOS·CPU·기본 포트·JSON 보강 원천 조사. 매핑 SQL의 정본은 유형 문서다.
-- name: computer-fields
SELECT d.device_pk,d.type,d.physicalsubtype,d.virtualsubtype,d.network_device,
d.name,d.notes,d.serial_no,d.uuid,d.last_discovered,
h.name AS hardware_name,v.name AS manufacturer,b.name AS bios_manufacturer,
d.bios_version,d.bios_revision,d.bios_fw_revision,d.bios_release_date,
d.ram,d.ram_size_type,d.total_cpus,d.core_per_cpu,d.threads_per_core,d.cpu_speed,d.hz,
d.os_architecture,d.vm_manager_int_id,d.vm_manager_ref_id,
d.details->>'arch' AS cloud_architecture,d.details->>'private_dns_name' AS private_dns_name,
d.details->>'public_dns_name' AS public_dns_name
FROM view_device_v2 d
LEFT JOIN view_hardware_v2 h ON h.hardware_pk=d.hardware_fk
LEFT JOIN view_vendor_v1 v ON v.vendor_pk=h.vendor_fk
LEFT JOIN view_vendor_v1 b ON b.vendor_pk=d.bios_vendor_fk
WHERE d.type IN ('physical','virtual')
AND (d.network_device=false OR d.network_device IS NULL)
AND (d.virtualsubtype IS NULL OR d.virtualsubtype<>'Docker Container')
AND (d.physicalsubtype IS NULL OR d.physicalsubtype NOT IN
('Network Printer','PDU','CRAC','UPS','Branch Circuit Power Meter','Power Unit','Environment Monitor','Access Point','TAP'))
ORDER BY d.device_pk;

-- name: cpu-details
SELECT p.device_fk,p.part_pk,p.pcount,pm.name,pm.cores,pm.speed,pm.speed_unit,
p.details->>'architecture' AS architecture,p.details->>'enabled_cores' AS enabled_cores,
p.details->>'number_of_logical_processors' AS logical_processors
FROM view_part_v1 p
JOIN view_partmodel_v1 pm ON pm.partmodel_pk=p.partmodel_fk
WHERE pm.type_name='CPU'
ORDER BY p.device_fk,p.part_pk;

-- name: default-ports
SELECT n.device_fk,n.netport_pk,n.port,n.hwaddress,n.is_default
FROM view_netport_v1 n
WHERE n.is_default=true
ORDER BY n.device_fk,n.netport_pk;
