-- DeployedAssetIntegrate 의 현행 필터와 ASSETCLASS 판정을 SQL 로 재현한다.
-- 코드 변경 시 이 쿼리도 함께 고친다.
-- 근거: DeployedAssetIntegrate.java DEVICE_FILTER, mapData()

-- name: etl-target
SELECT CASE
         WHEN d.network_device THEN 'NETDEVICE'
         WHEN d.physicalsubtype = 'Network Printer' THEN 'NETPRINTER'
         ELSE 'COMPUTER'
       END AS assetclass,
       d.type,
       COALESCE(d.virtualsubtype, d.physicalsubtype, '-') AS subtype,
       count(*) AS device_cnt
FROM view_device_v2 d
WHERE d.type IN ('virtual','physical')
  AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
GROUP BY 1, 2, 3
ORDER BY 1, 4 DESC;

-- name: etl-excluded
SELECT d.type,
       COALESCE(d.virtualsubtype, d.physicalsubtype, '-') AS subtype,
       count(*) AS device_cnt
FROM view_device_v2 d
WHERE NOT (d.type IN ('virtual','physical')
       AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15))
GROUP BY 1, 2
ORDER BY 3 DESC;
