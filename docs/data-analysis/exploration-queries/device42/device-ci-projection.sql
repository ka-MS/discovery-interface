-- Existing Computer joins only; candidate Device scope, no product code or target writes.
-- name: device-projection
WITH candidate AS (
SELECT d.* FROM view_device_v2 d WHERE (d.type IN ('physical','virtual') AND (d.network_device=false OR d.network_device IS NULL)
AND ((d.type='physical' AND d.physicalsubtype IN ('Generic','Rackable','Blade','WorkStation','ThinClient','Laptop'))
OR (d.type='virtual' AND d.virtualsubtype IN ('Internal VM','Amazon EC2 Instance','VMWare','Hyper-V'))))
OR (d.type='physical' AND (d.network_device=true OR d.physicalsubtype='Network Printer'))
), cpu AS (
SELECT p.device_fk, COUNT(DISTINCT NULLIF(TRIM(pm.name),'')) AS model_count,
MIN(NULLIF(TRIM(pm.name),'')) AS cpu_model,
COUNT(DISTINCT NULLIF(TRIM(p.details->>'architecture'),'')) AS arch_count,
MIN(NULLIF(TRIM(p.details->>'architecture'),'')) AS architecture,
COUNT(*) AS cpu_rows,SUM(p.pcount) AS cpu_quantity
FROM view_part_v1 p JOIN view_partmodel_v1 pm ON pm.partmodel_pk=p.partmodel_fk
JOIN candidate d ON d.device_pk=p.device_fk WHERE pm.type_name='CPU' GROUP BY p.device_fk
), primary_port AS (
SELECT n.device_fk,COUNT(*) AS default_port_count,
CASE WHEN COUNT(*)=1 THEN MIN(NULLIF(TRIM(n.hwaddress),'')) END AS primary_mac,
CASE WHEN COUNT(*)=1 THEN MIN(n.port) END AS primary_port,
CASE WHEN COUNT(*)=1 THEN MIN(n.name) END AS primary_port_name,
CASE WHEN COUNT(*)=1 THEN MIN(n.port_speed) END AS port_speed,
CASE WHEN COUNT(*)=1 THEN MIN(n.mtu) END AS mtu,
CASE WHEN COUNT(*)=1 THEN MIN(n.remote_netport_fk) END AS remote_netport_fk,
CASE WHEN COUNT(*)=1 THEN MIN(n.second_device_fk) END AS second_device_fk,
CASE WHEN COUNT(*)=1 THEN MIN(n.module_device_fk) END AS module_device_fk,
CASE WHEN COUNT(*)=1 THEN MIN(n.parent_part_fk) END AS parent_part_fk,
CASE WHEN COUNT(*)=1 THEN MIN(n.primary_vlan_fk) END AS primary_vlan_fk
FROM view_netport_v1 n JOIN candidate d ON d.device_pk=n.device_fk
WHERE n.is_default=true GROUP BY n.device_fk
), network_info AS (
SELECT n.second_device_fk AS physical_pk,
CASE WHEN COUNT(DISTINCT n.device_fk)=1 THEN MIN(n.device_fk) END AS cluster_pk,
COUNT(DISTINCT n.device_fk) AS cluster_count,
COUNT(DISTINCT NULLIF(TRIM(c.details->>'fw_device_type'),'')) AS network_kind_count,
MIN(NULLIF(TRIM(c.details->>'fw_device_type'),'')) AS network_kind,
MIN(NULLIF(TRIM(n.hwaddress),'')) AS network_mac
FROM view_netport_v1 n JOIN candidate d ON d.device_pk=n.second_device_fk
JOIN view_device_v2 c ON c.device_pk=n.device_fk
WHERE d.type='physical' AND d.network_device=true
AND c.type='cluster' AND c.network_device=true
GROUP BY n.second_device_fk
)
SELECT d.device_pk,
d.name,
d.type_id,
d.type,
d.serial_no,
d.asset_no,
d.uuid,
d.in_service,
d.service_level_id,
d.service_level,
d.first_added,
d.last_edited,
d.last_changed,
d.objectcategory_fk,
d.hardware_fk,
d.customer_fk,
d.host_chassis_device_fk,
d.blade_slot_no,
d.chassisslot_fk,
d.virtual_host_device_fk,
d.additional_location_info,
d.calculated_rack_fk,
d.rack_fk,
d.start_at,
d.orientation_id,
d.orientation,
d.where_id,
d.where,
d.reversed,
d.x_pos,
d.storage_room_fk,
d.room_fk,
d.calculated_room_fk,
d.grid_rows,
d.grid_cols,
d.building_fk,
d.physicalsubtype_fk,
d.physicalsubtype,
d.calculated_building_fk,
d.virtualsubtype_id,
d.virtualsubtype,
d.datacenter,
d.os_name,
d.deviceos_fk,
d.os_fk,
d.os_version,
d.os_version_no,
d.os_architecture_id,
d.os_architecture,
d.os_support_expires,
d.count_in_licensing,
d.os_first_added,
d.os_last_edited,
d.total_cpus,
d.core_per_cpu,
d.threads_per_core,
d.cpu_speed,
d.hz_id,
d.hz,
d.ram,
d.ram_size_type_id,
d.ram_size_type,
d.hard_disk_count,
d.hard_disk_size,
d.hard_disk_size_type_id,
d.hard_disk_size_type,
d.hw_sw_raid_id,
d.hw_sw_raid,
d.raid_type_id,
d.raid_type,
d.pdu_fk,
d.service_profile_name,
d.dn,
d.ucsmanager,
d.service_profile_description,
d.virtual_host,
d.network_device,
ni.cluster_pk,
ni.cluster_count,
ni.network_kind,
ni.network_kind_count,
ni.network_mac,
d.blade_chassis,
d.assetprofile_fk,
d.bios_version,
d.bios_revision,
d.bios_release_date,
d.bios_fw_revision,
d.bios_vendor_fk,
d.monitoring_enabled,
d.remotecollector_fk,
d.vm_manager_device_fk,
d.vm_manager_int_id,
d.vm_manager_ref_id,
d.vm_creation_date,
d.do_not_propagate,
d.last_discovered,
d.impact,
d.usage_type,
d.state,
CASE WHEN d.datastores IS NOT NULL THEN 1 ELSE 0 END AS has_datastores,
CASE WHEN d.details IS NOT NULL THEN 1 ELSE 0 END AS has_details,
CASE WHEN d.discovered_license_key IS NOT NULL THEN 1 ELSE 0 END AS has_discovered_license_key,
CASE WHEN d.ip_addresses IS NOT NULL THEN 1 ELSE 0 END AS has_ip_addresses,
CASE WHEN d.notes IS NOT NULL THEN 1 ELSE 0 END AS has_notes,
CASE WHEN d.os_license_key IS NOT NULL THEN 1 ELSE 0 END AS has_os_license_key,
CASE WHEN d.tags IS NOT NULL THEN 1 ELSE 0 END AS has_tags,
CASE WHEN d.vendor_custom_fields IS NOT NULL THEN 1 ELSE 0 END AS has_vendor_custom_fields,
h.name AS hardware_name,
h.physicalsubtype AS hardware_subtype,
h.network_device AS hardware_network_device,
h.part_number AS hardware_part_number,
h.size AS hardware_size,
h.depth AS hardware_depth,
h.watts AS hardware_watts,
h.end_of_life_date AS hardware_eol,
h.end_of_support_date AS hardware_eos,
h.specification_url AS hardware_spec_url,
v.name AS manufacturer,
v.home_page AS manufacturer_home_page,
b.name AS bios_manufacturer,
cpu.model_count,
cpu.cpu_model,
cpu.arch_count,
cpu.architecture AS cpu_architecture,
cpu.cpu_rows,
cpu.cpu_quantity,
pp.default_port_count,
pp.primary_mac,
pp.primary_port,
pp.primary_port_name,
pp.port_speed,
pp.mtu,
pp.remote_netport_fk,
pp.second_device_fk,
pp.module_device_fk,
pp.parent_part_fk,
pp.primary_vlan_fk,
CASE
WHEN d.network_device=true AND d.physicalsubtype='Network Printer' THEN 'CONFLICT'
WHEN d.type='physical' AND d.network_device=true AND ni.cluster_count=1
AND ni.network_kind_count=1 AND ni.network_kind='Switch' THEN 'SWITCH'
WHEN d.type='physical' AND d.network_device=true THEN 'UNRESOLVED_NETWORK'
WHEN d.type='physical' AND d.physicalsubtype='Network Printer' AND (d.network_device=false OR d.network_device IS NULL) THEN 'PRINTER'
WHEN d.type IN ('physical','virtual') AND (d.network_device=false OR d.network_device IS NULL)
AND ((d.type='physical' AND d.physicalsubtype IN ('Generic','Rackable','Blade','WorkStation','ThinClient','Laptop'))
OR (d.type='virtual' AND d.virtualsubtype IN ('Internal VM','Amazon EC2 Instance','VMWare','Hyper-V'))) THEN CASE WHEN d.type='virtual' THEN 'VIRTUAL_COMPUTER' ELSE 'COMPUTER' END
ELSE 'EXCLUDED' END AS device_kind
FROM candidate d
LEFT JOIN view_hardware_v2 h ON h.hardware_pk=d.hardware_fk
LEFT JOIN view_vendor_v1 v ON v.vendor_pk=h.vendor_fk
LEFT JOIN view_vendor_v1 b ON b.vendor_pk=d.bios_vendor_fk
LEFT JOIN cpu ON cpu.device_fk=d.device_pk
LEFT JOIN primary_port pp ON pp.device_fk=d.device_pk
LEFT JOIN network_info ni ON ni.physical_pk=d.device_pk
ORDER BY d.device_pk;
-- name: port-coverage
SELECT d.device_pk,d.type,d.physicalsubtype,d.network_device,
COUNT(n.netport_pk) AS all_ports,
SUM(CASE WHEN n.is_default=true THEN 1 ELSE 0 END) AS default_ports,
SUM(CASE WHEN n.remote_netport_fk IS NOT NULL THEN 1 ELSE 0 END) AS remote_links,
SUM(CASE WHEN n.second_device_fk IS NOT NULL THEN 1 ELSE 0 END) AS second_device_links
FROM view_device_v2 d LEFT JOIN view_netport_v1 n ON n.device_fk=d.device_pk
GROUP BY d.device_pk,d.type,d.physicalsubtype,d.network_device ORDER BY d.device_pk;
