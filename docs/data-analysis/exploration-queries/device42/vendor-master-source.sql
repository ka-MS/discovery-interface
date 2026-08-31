-- Maximo 제조사 변환(DPAMMANUFACTURER / DPAMMANUVARIANT)에 등록할 값의 원천을
-- 확인한다. Device42 는 벤더를 별도 뷰로 관리하고, 장비와 소프트웨어가 각각
-- vendor_fk 로 참조한다.
--
-- 블록
--   vendor-all        벤더 목록. 정규화 여부를 눈으로 확인한다
--   vendor-usage      vendor_fk 를 가진 뷰별 사용 벤더 수와 전체 행수
--   device-vendor     장비 type 별 제조사. hardware 를 거쳐야 얻어진다
--   software-vendor   소프트웨어 제조사 분포와 미지정 건수
--   enrich-coverage   enrichai_details 의 normalized/alias 충전율

-- name: vendor-all
SELECT vendor_pk, name
FROM view_vendor_v1
ORDER BY name;

-- name: vendor-usage
SELECT 'hardware' AS used_by,
       count(DISTINCT vendor_fk) AS vendors,
       count(*) AS rows_total
FROM view_hardware_v2
UNION ALL
SELECT 'software', count(DISTINCT vendor_fk), count(*)
FROM view_software_v1;

-- name: device-vendor
SELECT d.type,
       v.name AS vendor,
       count(*) AS n
FROM view_device_v2 d
LEFT JOIN view_hardware_v2 h ON h.hardware_pk = d.hardware_fk
LEFT JOIN view_vendor_v1   v ON v.vendor_pk   = h.vendor_fk
GROUP BY d.type, v.name
ORDER BY n DESC;

-- name: software-vendor
SELECT v.name AS vendor,
       count(*) AS n
FROM view_software_v1 s
LEFT JOIN view_vendor_v1 v ON v.vendor_pk = s.vendor_fk
GROUP BY v.name
ORDER BY n DESC;

-- name: enrich-coverage
SELECT count(*) AS vendors,
       sum(CASE WHEN enrichai_details::text LIKE '%normalized%' THEN 1 ELSE 0 END) AS with_normalized,
       sum(CASE WHEN enrichai_details::text LIKE '%alias%' THEN 1 ELSE 0 END) AS with_alias
FROM view_vendor_v1;
