-- DPASOFTWARE 의 COMPUTER 대상 원천과 값 분포를 확인한다. 대상 판정 조건은
-- DeployedAssetIntegrate 의 DEVICE_FILTER 와 자식 ASSETCLASS 조건을 그대로 쓴다.
--
-- 설명은 이 헤더에만 둔다. 실행기가 블록 본문을 한 줄로 이어 붙여서, 첫
-- `-- name:` 뒤의 주석은 앞 블록 SQL 을 깨뜨린다.
--
-- 블록
--   software-coverage  DPASOFTWARE 후보 컬럼별 충전 건수와 최대 길이
--   software-key       자연키 후보의 노드 내 유일성. 중복 제거 규칙 판단용
--   software-name-eq   softwareinuse.software_name 과 software.name 의 일치 여부
--   software-type      Device42 software_type 값 분포. 타겟 TYPE 과 다른 개념임을 확인

-- name: software-coverage
WITH src AS (
    SELECT u.install_path, u.version, u.install_date, u.first_detected,
        u.last_updated, u.alias_name, u.arch, u.enduser_fk,
        s.name AS catalog_name, s.description AS sw_desc,
        v.name AS vendor_name
    FROM view_softwareinuse_v1 u
    JOIN view_device_v2 d ON d.device_pk = u.device_fk
    LEFT JOIN view_software_v1 s ON s.software_pk = u.software_fk
    LEFT JOIN view_vendor_v1 v ON v.vendor_pk = s.vendor_fk
    WHERE d.type IN ('virtual', 'physical')
      AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
      AND (d.network_device = false OR d.network_device IS NULL)
      AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'Network Printer')
)
SELECT 'row-total' AS col, COUNT(*) AS filled, 0 AS max_len FROM src
UNION ALL SELECT 'catalog_name', COUNT(NULLIF(catalog_name,'')), MAX(LENGTH(catalog_name)) FROM src
UNION ALL SELECT 'version', COUNT(NULLIF(version,'')), MAX(LENGTH(version)) FROM src
UNION ALL SELECT 'install_path', COUNT(NULLIF(install_path,'')), MAX(LENGTH(install_path)) FROM src
UNION ALL SELECT 'vendor_name', COUNT(NULLIF(vendor_name,'')), MAX(LENGTH(vendor_name)) FROM src
UNION ALL SELECT 'sw_desc', COUNT(NULLIF(sw_desc,'')), MAX(LENGTH(sw_desc)) FROM src
UNION ALL SELECT 'alias_name', COUNT(NULLIF(alias_name,'')), MAX(LENGTH(alias_name)) FROM src
UNION ALL SELECT 'arch', COUNT(NULLIF(arch,'')), MAX(LENGTH(arch)) FROM src
UNION ALL SELECT 'install_date', COUNT(install_date), 0 FROM src
UNION ALL SELECT 'first_detected', COUNT(first_detected), 0 FROM src
UNION ALL SELECT 'last_updated', COUNT(last_updated), 0 FROM src
UNION ALL SELECT 'enduser_fk', COUNT(enduser_fk), 0 FROM src;

-- name: software-key
WITH src AS (
    SELECT u.device_fk, COALESCE(s.name, u.software_name) AS nm,
        COALESCE(u.version,'') AS ver, COALESCE(u.install_path,'') AS pth
    FROM view_softwareinuse_v1 u
    JOIN view_device_v2 d ON d.device_pk = u.device_fk
    LEFT JOIN view_software_v1 s ON s.software_pk = u.software_fk
    WHERE d.type IN ('virtual', 'physical')
      AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
      AND (d.network_device = false OR d.network_device IS NULL)
      AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'Network Printer')
),
g1 AS (SELECT device_fk, nm, ver, COUNT(*) AS n FROM src GROUP BY device_fk, nm, ver),
g2 AS (SELECT device_fk, nm, ver, pth, COUNT(*) AS n FROM src GROUP BY device_fk, nm, ver, pth)
SELECT (SELECT COUNT(*) FROM src) AS rows_all,
    (SELECT COUNT(*) FROM g1) AS k_name_ver,
    (SELECT MAX(n) FROM g1) AS max_dup_nv,
    (SELECT COUNT(*) FROM g2) AS k_plus_path,
    (SELECT MAX(n) FROM g2) AS max_dup_nvp;

-- name: software-name-eq
WITH src AS (
    SELECT u.software_name, s.name AS catalog_name
    FROM view_softwareinuse_v1 u
    JOIN view_device_v2 d ON d.device_pk = u.device_fk
    LEFT JOIN view_software_v1 s ON s.software_pk = u.software_fk
    WHERE d.type IN ('virtual', 'physical')
      AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
      AND (d.network_device = false OR d.network_device IS NULL)
      AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'Network Printer')
)
SELECT COUNT(*) AS row_cnt,
    COUNT(CASE WHEN software_name = catalog_name THEN 1 END) AS same_cnt,
    COUNT(CASE WHEN software_name <> catalog_name THEN 1 END) AS diff_cnt
FROM src;

-- name: software-type
SELECT COALESCE(NULLIF(s.software_type,''),'<EMPTY>') AS software_type,
    COALESCE(NULLIF(s.category_name,''),'<EMPTY>') AS category_name,
    COUNT(*) AS n
FROM view_softwareinuse_v1 u
JOIN view_software_v1 s ON s.software_pk = u.software_fk
GROUP BY s.software_type, s.category_name
ORDER BY n DESC
LIMIT 20;
