-- 기존 Device JSON은 값 대신 키와 빈도만 탐색한다. 별도 뷰를 조인하지 않는다.
-- name: details-keys
SELECT d.type,d.network_device,d.physicalsubtype,k,COUNT(*) AS cnt
FROM view_device_v2 d,LATERAL jsonb_object_keys(d.details::jsonb) AS k
GROUP BY d.type,d.network_device,d.physicalsubtype,k ORDER BY d.type,d.network_device,d.physicalsubtype,k;
-- name: network-printer-markers
SELECT device_pk,type,physicalsubtype,network_device,
details->>'switch_id' AS switch_id,
details->>'d42_device_classification' AS d42_device_classification,
details->>'device_type' AS detail_device_type,
details->>'fw_device_type' AS fw_device_type,
details->>'snmp_class' AS snmp_class,
details->>'json_node_type' AS json_node_type
FROM view_device_v2 WHERE network_device=true OR physicalsubtype='Network Printer'
ORDER BY device_pk;
-- name: custom-keys
SELECT d.type,d.network_device,d.physicalsubtype,k,COUNT(*) AS cnt
FROM view_device_v2 d,LATERAL jsonb_object_keys(d.vendor_custom_fields::jsonb) AS k
GROUP BY d.type,d.network_device,d.physicalsubtype,k ORDER BY d.type,d.network_device,d.physicalsubtype,k;
