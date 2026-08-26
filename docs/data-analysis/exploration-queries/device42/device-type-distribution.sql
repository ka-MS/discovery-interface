-- Device42 장비의 type/subtype 분포. ASSETCLASS 판정 근거다.

-- name: type-distribution
SELECT type,
       COALESCE(virtualsubtype, '-')  AS virtualsubtype,
       COALESCE(physicalsubtype, '-') AS physicalsubtype,
       virtual_host,
       network_device,
       count(*) AS device_cnt
FROM view_device_v2
GROUP BY 1, 2, 3, 4, 5
ORDER BY 6 DESC;

-- name: subtype-ids
SELECT virtualsubtype_id, virtualsubtype, count(*) AS cnt
FROM view_device_v2
WHERE virtualsubtype_id IS NOT NULL
GROUP BY 1, 2
ORDER BY 1;
