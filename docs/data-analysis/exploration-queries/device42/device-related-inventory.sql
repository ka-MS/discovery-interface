-- 장비 한 대의 연관 자원 건수를 뷰별로 집계한다.
-- 대상 변경: 아래 target CTE 의 name 값만 수정한다. pk 를 쓰지 않는다.

-- name: related-inventory
WITH target AS (
    SELECT device_pk AS pk FROM view_device_v2 WHERE name = 'episode'
)
SELECT 'view_deviceos_v1' AS related_view, 'device_fk' AS link_column, count(*) AS row_cnt
  FROM view_deviceos_v1 t, target d WHERE t.device_fk = d.pk
UNION ALL SELECT 'view_ipaddress_v1', 'device_fk', count(*)
  FROM view_ipaddress_v1 t, target d WHERE t.device_fk = d.pk
UNION ALL SELECT 'view_netport_v1', 'device_fk', count(*)
  FROM view_netport_v1 t, target d WHERE t.device_fk = d.pk
UNION ALL SELECT 'view_mountpoint_v1', 'device_fk', count(*)
  FROM view_mountpoint_v1 t, target d WHERE t.device_fk = d.pk
UNION ALL SELECT 'view_part_v1', 'device_fk', count(*)
  FROM view_part_v1 t, target d WHERE t.device_fk = d.pk
UNION ALL SELECT 'view_softwareinuse_v1', 'device_fk', count(*)
  FROM view_softwareinuse_v1 t, target d WHERE t.device_fk = d.pk
UNION ALL SELECT 'view_serviceinstance_v2', 'device_fk', count(*)
  FROM view_serviceinstance_v2 t, target d WHERE t.device_fk = d.pk
UNION ALL SELECT 'view_appcomp_v1', 'device_fk', count(*)
  FROM view_appcomp_v1 t, target d WHERE t.device_fk = d.pk
UNION ALL SELECT 'view_deviceurl_v1', 'device_fk', count(*)
  FROM view_deviceurl_v1 t, target d WHERE t.device_fk = d.pk
UNION ALL SELECT 'view_pdu_v1', 'device_fk', count(*)
  FROM view_pdu_v1 t, target d WHERE t.device_fk = d.pk
UNION ALL SELECT 'view_device_v2', 'virtual_host_device_fk', count(*)
  FROM view_device_v2 t, target d WHERE t.virtual_host_device_fk = d.pk
UNION ALL SELECT 'view_device_v2', 'host_chassis_device_fk', count(*)
  FROM view_device_v2 t, target d WHERE t.host_chassis_device_fk = d.pk
UNION ALL SELECT 'view_device_v2', 'vm_manager_device_fk', count(*)
  FROM view_device_v2 t, target d WHERE t.vm_manager_device_fk = d.pk
ORDER BY 3 DESC, 1, 2;
