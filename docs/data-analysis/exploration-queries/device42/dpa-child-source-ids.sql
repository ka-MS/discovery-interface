-- Maximo DPA 자체 ID로 직접 사용할 Device42 자식 PK 컬럼을 확인한다.

-- name: part-source-id
SELECT part_pk, device_fk
FROM view_part_v1
LIMIT 1;

-- name: mountpoint-source-id
SELECT mountpoint_pk, device_fk
FROM view_mountpoint_v1
LIMIT 1;

-- name: netport-source-id
SELECT netport_pk, device_fk
FROM view_netport_v1
LIMIT 1;

-- name: os-source-id
SELECT deviceos_pk, device_fk
FROM view_deviceos_v1
LIMIT 1;

-- name: software-source-id
SELECT softwareinuse_pk, device_fk
FROM view_softwareinuse_v1
LIMIT 1;

-- name: tcpip-source-id
SELECT ipaddress_pk, device_fk
FROM view_ipaddress_v1
LIMIT 1;
