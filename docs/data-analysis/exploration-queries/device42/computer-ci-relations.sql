-- 관계 조사: 본체의 DISTINCT ON을 적용하지 않고 연결 대상별 행을 보존한다.
-- name: component-edges
WITH computer AS (SELECT d.* FROM view_device_v2 d WHERE d.type IN ('physical','virtual')
AND (d.network_device=false OR d.network_device IS NULL)
AND ((d.type='physical' AND d.physicalsubtype IN ('Generic','Rackable','Blade','WorkStation','ThinClient','Laptop'))
OR (d.type='virtual' AND d.virtualsubtype IN ('Internal VM','Amazon EC2 Instance','VMWare','Hyper-V'))))
SELECT 'DISK' AS kind, p.part_pk AS child_pk, c.device_pk AS computer_pk, c.type AS computer_type
FROM view_part_v1 p JOIN view_partmodel_v1 pm ON pm.partmodel_pk=p.partmodel_fk
JOIN computer c ON c.device_pk=p.device_fk WHERE pm.type_name='Hard Disk'
UNION ALL
SELECT 'OS',o.deviceos_pk,c.device_pk,c.type FROM view_deviceos_v1 o JOIN computer c ON c.device_pk=o.device_fk
UNION ALL
SELECT 'FILESYSTEM',m.mountpoint_pk,c.device_pk,c.type FROM view_mountpoint_v2 m JOIN computer c ON c.device_pk=ANY(m.device_fks)
WHERE (m.fstype_name IS NULL OR m.fstype_name NOT IN ('overlay','devtmpfs','squashfs','efivarfs'))
UNION ALL
SELECT 'IP',i.ipaddress_pk,c.device_pk,c.type FROM view_ipaddress_v2 i JOIN computer c ON c.device_pk=ANY(i.device_fks)
ORDER BY kind,child_pk,computer_pk;

-- name: device-links
WITH computer AS (SELECT d.* FROM view_device_v2 d WHERE d.type IN ('physical','virtual')
AND (d.network_device=false OR d.network_device IS NULL)
AND ((d.type='physical' AND d.physicalsubtype IN ('Generic','Rackable','Blade','WorkStation','ThinClient','Laptop'))
OR (d.type='virtual' AND d.virtualsubtype IN ('Internal VM','Amazon EC2 Instance','VMWare','Hyper-V'))))
SELECT 'VIRTUAL_HOST' AS kind,c.device_pk AS child_pk,c.type AS child_type,c.virtualsubtype,
h.device_pk AS parent_pk,h.type AS parent_type,h.physicalsubtype,h.virtualsubtype AS parent_virtualsubtype,
CASE WHEN p.device_pk IS NULL THEN 0 ELSE 1 END AS parent_in_scope
FROM computer c JOIN view_device_v2 h ON h.device_pk=c.virtual_host_device_fk LEFT JOIN computer p ON p.device_pk=h.device_pk
UNION ALL
SELECT 'CHASSIS',c.device_pk,c.type,c.virtualsubtype,h.device_pk,h.type,h.physicalsubtype,h.virtualsubtype,
CASE WHEN p.device_pk IS NULL THEN 0 ELSE 1 END
FROM computer c JOIN view_device_v2 h ON h.device_pk=c.host_chassis_device_fk LEFT JOIN computer p ON p.device_pk=h.device_pk
UNION ALL
SELECT 'VM_MANAGER',c.device_pk,c.type,c.virtualsubtype,h.device_pk,h.type,h.physicalsubtype,h.virtualsubtype,
CASE WHEN p.device_pk IS NULL THEN 0 ELSE 1 END
FROM computer c JOIN view_device_v2 h ON h.device_pk=c.vm_manager_device_fk LEFT JOIN computer p ON p.device_pk=h.device_pk
ORDER BY kind,child_pk,parent_pk;

-- name: ip-interface-paths
WITH computer AS (SELECT d.* FROM view_device_v2 d WHERE d.type IN ('physical','virtual')
AND (d.network_device=false OR d.network_device IS NULL)
AND ((d.type='physical' AND d.physicalsubtype IN ('Generic','Rackable','Blade','WorkStation','ThinClient','Laptop'))
OR (d.type='virtual' AND d.virtualsubtype IN ('Internal VM','Amazon EC2 Instance','VMWare','Hyper-V'))))
SELECT i.ipaddress_pk,c.device_pk AS computer_pk,i.netport_fk,n.device_fk AS port_device_fk,
CASE WHEN pc.device_pk IS NULL THEN 0 ELSE 1 END AS port_device_in_scope,i.is_shared
FROM view_ipaddress_v2 i JOIN computer c ON c.device_pk=ANY(i.device_fks)
LEFT JOIN view_netport_v1 n ON n.netport_pk=i.netport_fk
LEFT JOIN computer pc ON pc.device_pk=n.device_fk
ORDER BY i.ipaddress_pk,c.device_pk;

-- name: interface-edges
WITH computer AS (SELECT d.* FROM view_device_v2 d WHERE d.type IN ('physical','virtual')
AND (d.network_device=false OR d.network_device IS NULL)
AND ((d.type='physical' AND d.physicalsubtype IN ('Generic','Rackable','Blade','WorkStation','ThinClient','Laptop'))
OR (d.type='virtual' AND d.virtualsubtype IN ('Internal VM','Amazon EC2 Instance','VMWare','Hyper-V'))))
SELECT n.netport_pk,n.device_fk,n.second_device_fk FROM view_netport_v1 n
JOIN computer c ON c.device_pk=n.device_fk ORDER BY n.netport_pk;
-- name: missing-device-links
WITH computer AS (SELECT d.* FROM view_device_v2 d WHERE d.type IN ('physical','virtual')
AND (d.network_device=false OR d.network_device IS NULL)
AND ((d.type='physical' AND d.physicalsubtype IN ('Generic','Rackable','Blade','WorkStation','ThinClient','Laptop'))
OR (d.type='virtual' AND d.virtualsubtype IN ('Internal VM','Amazon EC2 Instance','VMWare','Hyper-V'))))
SELECT c.device_pk,c.virtual_host_device_fk,c.host_chassis_device_fk,c.vm_manager_device_fk
FROM computer c
WHERE (c.virtual_host_device_fk IS NOT NULL AND NOT EXISTS (SELECT 1 FROM view_device_v2 d WHERE d.device_pk=c.virtual_host_device_fk))
OR (c.host_chassis_device_fk IS NOT NULL AND NOT EXISTS (SELECT 1 FROM view_device_v2 d WHERE d.device_pk=c.host_chassis_device_fk))
OR (c.vm_manager_device_fk IS NOT NULL AND NOT EXISTS (SELECT 1 FROM view_device_v2 d WHERE d.device_pk=c.vm_manager_device_fk))
ORDER BY c.device_pk;
-- name: os-filesystem-candidates
WITH computer AS (SELECT d.* FROM view_device_v2 d WHERE d.type IN ('physical','virtual')
AND (d.network_device=false OR d.network_device IS NULL)
AND ((d.type='physical' AND d.physicalsubtype IN ('Generic','Rackable','Blade','WorkStation','ThinClient','Laptop'))
OR (d.type='virtual' AND d.virtualsubtype IN ('Internal VM','Amazon EC2 Instance','VMWare','Hyper-V'))))
SELECT o.deviceos_pk,c.device_pk,m.mountpoint_pk,m.mountpoint,m.fstype_name
FROM view_deviceos_v1 o JOIN computer c ON c.device_pk=o.device_fk
JOIN view_mountpoint_v2 m ON c.device_pk=ANY(m.device_fks)
WHERE (m.fstype_name IS NULL OR m.fstype_name NOT IN ('overlay','devtmpfs','squashfs','efivarfs'))
ORDER BY o.deviceos_pk,m.mountpoint_pk;
-- name: mapped-computer-relations
WITH computer AS (SELECT d.* FROM view_device_v2 d WHERE d.type IN ('physical','virtual')
AND (d.network_device=false OR d.network_device IS NULL)
AND ((d.type='physical' AND d.physicalsubtype IN ('Generic','Rackable','Blade','WorkStation','ThinClient','Laptop'))
OR (d.type='virtual' AND d.virtualsubtype IN ('Internal VM','Amazon EC2 Instance','VMWare','Hyper-V'))))
SELECT DISTINCT 'D42:DEVICE:' || CAST(c.device_pk AS varchar) AS sourceci,
       'D42:PART:' || CAST(p.part_pk AS varchar) AS targetci,
       'RELATION.CONTAINS' AS relationnum
FROM view_part_v1 p
JOIN view_partmodel_v1 pm ON pm.partmodel_pk=p.partmodel_fk
JOIN computer c ON c.device_pk=p.device_fk
WHERE pm.type_name='Hard Disk'
UNION ALL
SELECT DISTINCT 'D42:DEVICE:' || CAST(c.device_pk AS varchar),
       'D42:MOUNTPOINT:' || CAST(m.mountpoint_pk AS varchar),
       'RELATION.CONTAINS'
FROM view_mountpoint_v2 m
JOIN computer c ON c.device_pk=ANY(m.device_fks)
WHERE (m.fstype_name IS NULL OR m.fstype_name NOT IN ('overlay','devtmpfs','squashfs','efivarfs'))
ORDER BY sourceci,targetci;
-- name: mapped-os-relations
WITH computer AS (SELECT d.* FROM view_device_v2 d WHERE d.type IN ('physical','virtual')
AND (d.network_device=false OR d.network_device IS NULL)
AND ((d.type='physical' AND d.physicalsubtype IN ('Generic','Rackable','Blade','WorkStation','ThinClient','Laptop'))
OR (d.type='virtual' AND d.virtualsubtype IN ('Internal VM','Amazon EC2 Instance','VMWare','Hyper-V'))))
SELECT DISTINCT 'D42:DEVICEOS:' || CAST(o.deviceos_pk AS varchar) AS sourceci,
       'D42:DEVICE:' || CAST(c.device_pk AS varchar) AS targetci,
       'RELATION.INSTALLEDON' AS relationnum
FROM view_deviceos_v1 o
JOIN computer c ON c.device_pk=o.device_fk
ORDER BY sourceci,targetci;

-- name: mapped-host-vm-relations
WITH computer AS (SELECT d.device_pk,d.type,d.virtual_host_device_fk FROM view_device_v2 d
WHERE d.type IN ('physical','virtual')
AND (d.network_device=false OR d.network_device IS NULL)
AND ((d.type='physical' AND d.physicalsubtype IN ('Generic','Rackable','Blade','WorkStation','ThinClient','Laptop'))
OR (d.type='virtual' AND d.virtualsubtype IN ('Internal VM','Amazon EC2 Instance','VMWare','Hyper-V'))))
SELECT 'D42:DEVICE:' || CAST(host.device_pk AS varchar) AS sourceci,
       'D42:DEVICE:' || CAST(vm.device_pk AS varchar) AS targetci,
       'VIRTUALIZES' AS relationnum
FROM computer vm
JOIN computer host ON host.device_pk=vm.virtual_host_device_fk
WHERE vm.type='virtual' AND host.device_pk<>vm.device_pk
ORDER BY sourceci,targetci;

-- name: mapped-host-vm-count
WITH computer AS (SELECT d.device_pk,d.type,d.virtual_host_device_fk FROM view_device_v2 d
WHERE d.type IN ('physical','virtual')
AND (d.network_device=false OR d.network_device IS NULL)
AND ((d.type='physical' AND d.physicalsubtype IN ('Generic','Rackable','Blade','WorkStation','ThinClient','Laptop'))
OR (d.type='virtual' AND d.virtualsubtype IN ('Internal VM','Amazon EC2 Instance','VMWare','Hyper-V'))))
SELECT COUNT(*)
FROM computer vm
JOIN computer host ON host.device_pk=vm.virtual_host_device_fk
WHERE vm.type='virtual' AND host.device_pk<>vm.device_pk;
