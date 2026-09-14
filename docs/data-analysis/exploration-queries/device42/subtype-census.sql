-- 타입 계열 전수 확인. type·physicalsubtype·virtualsubtype·assettype 과
-- 판정에 쓸 수 있는 플래그를 본다. 서버를 바꿔 두 번 실행한다.

-- name: type-id-values
SELECT type_id, type, COUNT(*) AS cnt
FROM view_device_v2
GROUP BY type_id, type
ORDER BY type_id;

-- name: type-subtype-census
SELECT type,
       COALESCE(physicalsubtype, '-') AS physicalsubtype,
       COALESCE(virtualsubtype, '-') AS virtualsubtype,
       virtualsubtype_id,
       network_device,
       virtual_host,
       COUNT(*) AS device_cnt
FROM view_device_v2
GROUP BY type, physicalsubtype, virtualsubtype, virtualsubtype_id,
         network_device, virtual_host
ORDER BY type, physicalsubtype, virtualsubtype;

-- name: physicalsubtype-flag-signature
SELECT physicalsubtype_pk AS pk, physicalsubtype_name AS name,
       storage_room, server_room, building, rack, chassis,
       system_generated,
       (SELECT COUNT(*) FROM view_device_v2 d
          WHERE d.physicalsubtype_fk = p.physicalsubtype_pk) AS devices,
       (SELECT COUNT(*) FROM view_hardware_v2 h
          WHERE h.physicalsubtype_fk = p.physicalsubtype_pk) AS hardware
FROM view_physicalsubtype_v2 p
ORDER BY physicalsubtype_pk;

-- name: virtualsubtype-pairs
SELECT virtualsubtype_id, virtualsubtype, COUNT(*) AS cnt
FROM view_device_v2
WHERE virtualsubtype_id IS NOT NULL
GROUP BY virtualsubtype_id, virtualsubtype
ORDER BY virtualsubtype_id;

-- name: assettype-master
SELECT assettype_pk, name, storage_room, server_room, building, rack,
       location, device_relation
FROM view_assettype_v1
ORDER BY assettype_pk;

-- name: asset-and-pdu-rows
SELECT 'view_asset_v1' AS view_name, COUNT(*) AS row_cnt FROM view_asset_v1
UNION ALL SELECT 'view_pdu_v1', COUNT(*) FROM view_pdu_v1
UNION ALL SELECT 'view_pdumodel_v1', COUNT(*) FROM view_pdumodel_v1
UNION ALL SELECT 'view_objectcategory_v1', COUNT(*) FROM view_objectcategory_v1;

-- name: nonstandard-type-sample
SELECT device_pk, name, type, physicalsubtype, virtualsubtype,
       network_device, virtual_host, hardware_fk, serial_no, last_discovered
FROM view_device_v2
WHERE type NOT IN ('virtual', 'physical')
ORDER BY device_pk
LIMIT 25;
