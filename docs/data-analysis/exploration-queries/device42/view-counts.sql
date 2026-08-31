-- 실재 확인된 DOQL 뷰의 건수. 서버 간 데이터 성격 비교에 쓴다.

-- name: view-counts
SELECT 'view_device_v2' AS view_name, count(*) AS cnt FROM view_device_v2
UNION ALL SELECT 'view_hardware_v2', count(*) FROM view_hardware_v2
UNION ALL SELECT 'view_vendor_v1', count(*) FROM view_vendor_v1
UNION ALL SELECT 'view_software_v1', count(*) FROM view_software_v1
UNION ALL SELECT 'view_softwareinuse_v1', count(*) FROM view_softwareinuse_v1
UNION ALL SELECT 'view_serviceinstance_v2', count(*) FROM view_serviceinstance_v2
UNION ALL SELECT 'view_service_v2', count(*) FROM view_service_v2
UNION ALL SELECT 'view_appcomp_v1', count(*) FROM view_appcomp_v1
UNION ALL SELECT 'view_netport_v1', count(*) FROM view_netport_v1
UNION ALL SELECT 'view_ipaddress_v2', count(*) FROM view_ipaddress_v2
UNION ALL SELECT 'view_subnet_v1', count(*) FROM view_subnet_v1
UNION ALL SELECT 'view_vlan_v1', count(*) FROM view_vlan_v1
UNION ALL SELECT 'view_part_v1', count(*) FROM view_part_v1
UNION ALL SELECT 'view_partmodel_v1', count(*) FROM view_partmodel_v1
UNION ALL SELECT 'view_mountpoint_v2', count(*) FROM view_mountpoint_v2
UNION ALL SELECT 'view_deviceos_v1', count(*) FROM view_deviceos_v1
UNION ALL SELECT 'view_os_v1', count(*) FROM view_os_v1
UNION ALL SELECT 'view_deviceurl_v1', count(*) FROM view_deviceurl_v1
UNION ALL SELECT 'view_pdu_v1', count(*) FROM view_pdu_v1
UNION ALL SELECT 'view_rack_v1', count(*) FROM view_rack_v1
UNION ALL SELECT 'view_room_v1', count(*) FROM view_room_v1
UNION ALL SELECT 'view_building_v1', count(*) FROM view_building_v1
UNION ALL SELECT 'view_asset_v1', count(*) FROM view_asset_v1
UNION ALL SELECT 'view_customer_v1', count(*) FROM view_customer_v1
UNION ALL SELECT 'view_enduser_v1', count(*) FROM view_enduser_v1
UNION ALL SELECT 'view_remotecollector_v1', count(*) FROM view_remotecollector_v1
ORDER BY 2 DESC;

-- name: part-types
SELECT pm.type_name, count(*) AS part_cnt, count(DISTINCT p.device_fk) AS device_cnt
FROM view_part_v1 p
LEFT JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
GROUP BY pm.type_name
ORDER BY 2 DESC;
