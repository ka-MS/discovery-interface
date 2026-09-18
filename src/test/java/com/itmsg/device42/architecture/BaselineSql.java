package com.itmsg.device42.architecture;

import java.util.List;
import java.util.stream.Collectors;

/** 기준 8cabf920의 SQL 리터럴 동결본. 현재 구현에서 생성하지 않는다. 운영 코드가 아니다. */
final class BaselineSql {
    static final class DpamManufacturerQuery {
static final String PARENT_FILTER = """
            d.type IN ('virtual', 'physical')
            AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
            AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'PDU')
            """;

static final String COMPUTER_FILTER = """
            d.type IN ('virtual', 'physical')
            AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype NOT IN ('Network Printer', 'PDU')
            )
            """;

static final String SOURCE_CTE = """
            WITH target AS (
                SELECT d.device_pk, d.hardware_fk
                FROM view_device_v2 d
                WHERE
            """ + PARENT_FILTER + """
            ),
            computer AS (
                SELECT d.device_pk
                FROM view_device_v2 d
                WHERE
            """ + COMPUTER_FILTER + """
            ),
            names AS (
                SELECT v.name FROM target t
                JOIN view_hardware_v2 h ON h.hardware_pk = t.hardware_fk
                JOIN view_vendor_v1 v ON v.vendor_pk = h.vendor_fk
                UNION
                SELECT v.name FROM view_part_v1 p
                JOIN computer c ON c.device_pk = p.device_fk
                JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
                JOIN view_vendor_v1 v ON v.vendor_pk = pm.vendor_fk
                UNION
                SELECT v.name FROM view_deviceos_v1 o
                JOIN computer c ON c.device_pk = o.device_fk
                JOIN view_os_v1 s ON s.os_pk = o.os_fk
                JOIN view_vendor_v1 v ON v.vendor_pk = s.vendor_fk
                UNION
                SELECT v.name FROM view_netport_v1 n
                JOIN computer c ON c.device_pk = n.device_fk
                JOIN view_vendor_v1 v ON v.vendor_pk = n.vendor_fk
                UNION
                SELECT v.name FROM view_softwareinuse_v1 u
                JOIN computer c ON c.device_pk = u.device_fk
                JOIN view_software_v1 sw ON sw.software_pk = u.software_fk
                JOIN view_vendor_v1 v ON v.vendor_pk = sw.vendor_fk
                UNION
                SELECT 'UNKNOWN'
            ),
            filtered AS (
                SELECT name FROM names WHERE name IS NOT NULL AND name <> ''
            )
            """;

static final String TOTAL_COUNT_QUERY = SOURCE_CTE + """
            SELECT COUNT(*) FROM filtered
            """;

static final String SOURCE_QUERY = SOURCE_CTE + """
            SELECT name FROM filtered ORDER BY name
            """;
    }
    static final class DpamManuVariantQuery {
static final String PARENT_FILTER = """
            d.type IN ('virtual', 'physical')
            AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
            AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'PDU')
            """;

static final String COMPUTER_FILTER = """
            d.type IN ('virtual', 'physical')
            AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype NOT IN ('Network Printer', 'PDU')
            )
            """;

static final String SOURCE_CTE = """
            WITH target AS (
                SELECT d.device_pk, d.hardware_fk
                FROM view_device_v2 d
                WHERE
            """ + PARENT_FILTER + """
            ),
            computer AS (
                SELECT d.device_pk
                FROM view_device_v2 d
                WHERE
            """ + COMPUTER_FILTER + """
            ),
            names AS (
                SELECT v.name FROM target t
                JOIN view_hardware_v2 h ON h.hardware_pk = t.hardware_fk
                JOIN view_vendor_v1 v ON v.vendor_pk = h.vendor_fk
                UNION
                SELECT v.name FROM view_part_v1 p
                JOIN computer c ON c.device_pk = p.device_fk
                JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
                JOIN view_vendor_v1 v ON v.vendor_pk = pm.vendor_fk
                UNION
                SELECT v.name FROM view_deviceos_v1 o
                JOIN computer c ON c.device_pk = o.device_fk
                JOIN view_os_v1 s ON s.os_pk = o.os_fk
                JOIN view_vendor_v1 v ON v.vendor_pk = s.vendor_fk
                UNION
                SELECT v.name FROM view_netport_v1 n
                JOIN computer c ON c.device_pk = n.device_fk
                JOIN view_vendor_v1 v ON v.vendor_pk = n.vendor_fk
                UNION
                SELECT v.name FROM view_softwareinuse_v1 u
                JOIN computer c ON c.device_pk = u.device_fk
                JOIN view_software_v1 sw ON sw.software_pk = u.software_fk
                JOIN view_vendor_v1 v ON v.vendor_pk = sw.vendor_fk
                UNION
                SELECT 'UNKNOWN'
            ),
            filtered AS (
                SELECT name FROM names WHERE name IS NOT NULL AND name <> ''
            )
            """;

static final String TOTAL_COUNT_QUERY = SOURCE_CTE + """
            SELECT COUNT(*) FROM filtered
            """;

static final String SOURCE_QUERY = SOURCE_CTE + """
            SELECT name FROM filtered ORDER BY name
            """;
    }
    static final class DpamProcessorQuery {
static final String SOURCE_QUERY = """
            SELECT DISTINCT pm.name
            FROM view_part_v1 p
            JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
            JOIN view_device_v2 d ON d.device_pk = p.device_fk
            WHERE pm.type_name = 'CPU'
              AND
            d.type IN ('virtual', 'physical')
            AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype NOT IN ('Network Printer', 'PDU')
            )
              AND pm.name IS NOT NULL
              AND pm.name <> ''
            """;

static final String TOTAL_COUNT_QUERY = """
            WITH source AS (
""" + SOURCE_QUERY + """
            )
            SELECT COUNT(*) FROM source
            """;
    }
    static final class DpamProcVariantQuery {
static final String SOURCE_QUERY = """
            SELECT DISTINCT pm.name
            FROM view_part_v1 p
            JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
            JOIN view_device_v2 d ON d.device_pk = p.device_fk
            WHERE pm.type_name = 'CPU'
              AND
            d.type IN ('virtual', 'physical')
            AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype NOT IN ('Network Printer', 'PDU')
            )
              AND pm.name IS NOT NULL
              AND pm.name <> ''
            """;

static final String TOTAL_COUNT_QUERY = """
            WITH source AS (
""" + SOURCE_QUERY + """
            )
            SELECT COUNT(*) FROM source
            """;
    }
    static final class DpamOsQuery {
static final String SOURCE_QUERY = """
            SELECT DISTINCT o.os_name AS name
            FROM view_deviceos_v1 o
            JOIN view_device_v2 d ON d.device_pk = o.device_fk
            WHERE
            d.type IN ('virtual', 'physical')
            AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype NOT IN ('Network Printer', 'PDU')
            )
              AND o.os_name IS NOT NULL
              AND o.os_name <> ''
            """;

static final String TOTAL_COUNT_QUERY = """
            WITH source AS (
""" + SOURCE_QUERY + """
            )
            SELECT COUNT(*) FROM source
            """;
    }
    static final class DpamOsVariantQuery {
static final String SOURCE_QUERY = """
            SELECT DISTINCT o.os_name AS name
            FROM view_deviceos_v1 o
            JOIN view_device_v2 d ON d.device_pk = o.device_fk
            WHERE
            d.type IN ('virtual', 'physical')
            AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype NOT IN ('Network Printer', 'PDU')
            )
              AND o.os_name IS NOT NULL
              AND o.os_name <> ''
            """;

static final String TOTAL_COUNT_QUERY = """
            WITH source AS (
""" + SOURCE_QUERY + """
            )
            SELECT COUNT(*) FROM source
            """;
    }
    static final class DpamAdapterQuery {
static final String SOURCE_QUERY = """
            WITH gpu AS (
                SELECT DISTINCT pm.name
                FROM view_part_v1 p
                JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
                JOIN view_device_v2 d ON d.device_pk = p.device_fk
                WHERE pm.type_name = 'GPU'
                  AND
            d.type IN ('virtual', 'physical')
            AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype NOT IN ('Network Printer', 'PDU')
            )
                  AND pm.name IS NOT NULL
                  AND pm.name <> ''
            )
            SELECT name FROM gpu
            UNION
            SELECT 'UNKNOWN'
            """;

static final String TOTAL_COUNT_QUERY = """
            WITH source AS (
""" + SOURCE_QUERY + """
            )
            SELECT COUNT(*) FROM source
            """;
    }
    static final class DpamAdptVariantQuery {
static final String SOURCE_QUERY = """
            WITH gpu AS (
                SELECT DISTINCT pm.name
                FROM view_part_v1 p
                JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
                JOIN view_device_v2 d ON d.device_pk = p.device_fk
                WHERE pm.type_name = 'GPU'
                  AND
            d.type IN ('virtual', 'physical')
            AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype NOT IN ('Network Printer', 'PDU')
            )
                  AND pm.name IS NOT NULL
                  AND pm.name <> ''
            )
            SELECT name FROM gpu
            UNION
            SELECT 'UNKNOWN'
            """;

static final String TOTAL_COUNT_QUERY = """
            WITH source AS (
""" + SOURCE_QUERY + """
            )
            SELECT COUNT(*) FROM source
            """;
    }
    static final class NetPrinterQuery {
static final String DEVICE_TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            FROM view_device_v2 d
            WHERE d.type IN ('virtual', 'physical')
              AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
              AND (d.network_device = false OR d.network_device IS NULL)
              AND d.physicalsubtype = 'Network Printer'
            """;

static final String DEVICE_QUERY = """
            SELECT
                d.device_pk,
                d.ram,
                d.ram_size_type,
                (SELECT UPPER(n.hwaddress)
                 FROM view_netport_v1 n
                 WHERE n.device_fk = d.device_pk AND n.hwaddress <> ''
                 LIMIT 1) AS hwaddress,
                (SELECT HOST(i.ip_address)
                 FROM view_ipaddress_v2 i
                 WHERE d.device_pk = ANY(i.device_fks)
                 LIMIT 1) AS ip_address,
                (SELECT COUNT(*)
                 FROM view_part_v1 p
                 JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
                 WHERE p.device_fk = d.device_pk AND pm.type_name = 'printer_input') AS tray_cnt
            FROM view_device_v2 d
            WHERE d.type IN ('virtual', 'physical')
              AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
              AND (d.network_device = false OR d.network_device IS NULL)
              AND d.physicalsubtype = 'Network Printer'
            ORDER BY d.device_pk
            """;
    }
    static final class MediaAdapterQuery {
static final String DEVICE_FILTER = """
            d.type IN ('virtual', 'physical')
            AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype NOT IN ('Network Printer', 'PDU')
            )
            """;

static final String SOURCE_FROM_AND_FILTER = """
            FROM view_part_v1 p
            JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
            JOIN view_device_v2 d ON d.device_pk = p.device_fk
            LEFT JOIN view_vendor_v1 v ON v.vendor_pk = pm.vendor_fk
            WHERE pm.type_name = 'GPU'
              AND
            """ + DEVICE_FILTER;

static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            """ + SOURCE_FROM_AND_FILTER;

static final String SOURCE_QUERY = """
            SELECT
                p.part_pk,
                p.device_fk,
                p.serial_no,
                p.description,
                pm.name AS model_name,
                pm.description AS model_description,
                v.name AS vendor_name
            """ + SOURCE_FROM_AND_FILTER + """
            ORDER BY p.device_fk, p.part_pk
            """;
    }
    static final class NetDeviceQuery {
static final String DEVICE_TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            FROM view_device_v2 d
            WHERE d.network_device = true
              AND d.type = 'physical'
            """;

static final String DEVICE_QUERY = """
            WITH target AS (
                SELECT device_pk, os_version
                FROM view_device_v2
                WHERE network_device = true
                  AND type = 'physical'
            ),
            link AS (
                SELECT second_device_fk AS physical_pk,
                       device_fk AS cluster_pk,
                       MIN(hwaddress) AS mac
                FROM view_netport_v1
                WHERE second_device_fk IS NOT NULL
                  AND hwaddress IS NOT NULL
                  AND hwaddress <> ''
                GROUP BY second_device_fk, device_fk
            )
            SELECT t.device_pk,
                   t.os_version,
                   l.mac,
                   (SELECT MIN(ip_address)
                      FROM view_ipaddress_v2
                     WHERE l.cluster_pk = ANY(device_fks)) AS mgmt_ip
            FROM target t
            LEFT JOIN link l ON l.physical_pk = t.device_pk
            ORDER BY t.device_pk
            """;
    }
    static final class DeployedAssetQuery {
static final String DEVICE_FILTER = """
            d.type IN ('virtual', 'physical')
            AND (
                d.virtualsubtype_id IS NULL
                OR d.virtualsubtype_id <> 15
            )
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype <> 'PDU'
            )
            """;

static final String DEVICE_TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            FROM view_device_v2 d
            WHERE
            """ + DEVICE_FILTER;

static final String DEVICE_QUERY = """
            SELECT
                d.device_pk,
                d.name,
                d.type,
                d.notes,
                d.serial_no,
                d.asset_no,
                d.uuid,
                d.network_device,
                d.physicalsubtype,
                h.name AS hardware_name,
                v.name AS vendor_name,
                d.in_service,
                d.last_discovered
            FROM view_device_v2 d
            LEFT JOIN view_hardware_v2 h
                ON d.hardware_fk = h.hardware_pk
            LEFT JOIN view_vendor_v1 v
                ON h.vendor_fk = v.vendor_pk
            WHERE
            """ + DEVICE_FILTER + """
            ORDER BY d.device_pk
            """;
    }
    static final class DiskQuery {
static final String DEVICE_FILTER = """
            d.type IN ('virtual', 'physical')
            AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype NOT IN ('Network Printer', 'PDU')
            )
            """;

static final String SOURCE_FROM_AND_FILTER = """
            FROM view_part_v1 p
            JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
            JOIN view_device_v2 d ON d.device_pk = p.device_fk
            LEFT JOIN view_vendor_v1 v ON v.vendor_pk = pm.vendor_fk
            WHERE pm.type_name = 'Hard Disk'
              AND
            """ + DEVICE_FILTER;

static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            """ + SOURCE_FROM_AND_FILTER;

static final String SOURCE_QUERY = """
            SELECT
                p.part_pk,
                p.device_fk,
                p.serial_no,
                p.description,
                pm.name AS model_name,
                pm.hdsize,
                pm.hdsize_unit,
                pm.hddtype_name,
                pm.media_type_name,
                v.name AS vendor_name
            """ + SOURCE_FROM_AND_FILTER + """
            ORDER BY p.device_fk, pm.name, p.part_pk
            """;
    }
    static final class ComputerQuery {
static final String DEVICE_FILTER = """
            d.type IN ('virtual', 'physical')
            AND (
                d.virtualsubtype_id IS NULL
                OR d.virtualsubtype_id <> 15
            )
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype NOT IN ('Network Printer', 'PDU')
            )
            """;

static final String DEVICE_TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            FROM view_device_v2 d
            WHERE
            """ + DEVICE_FILTER;

static final String DEVICE_QUERY = """
            SELECT
                d.device_pk,
                v.name AS bios_name,
                d.bios_version,
                d.bios_release_date,
                d.ram,
                d.ram_size_type,
                d.total_cpus,
                d.core_per_cpu
            FROM view_device_v2 d
            LEFT JOIN view_vendor_v1 v
                ON v.vendor_pk = d.bios_vendor_fk
            WHERE
            """ + DEVICE_FILTER + """
            ORDER BY d.device_pk
            """;
    }
    static final class LogicalDriveQuery {
static final String DEVICE_FILTER = """
            d.type IN ('virtual', 'physical')
            AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype NOT IN ('Network Printer', 'PDU')
            )
            """;

static final String SOURCE_FROM_AND_FILTER = """
            FROM view_mountpoint_v2 m
            JOIN view_device_v2 d ON d.device_pk = ANY(m.device_fks)
            WHERE
            """ + DEVICE_FILTER + """
              AND LOWER(COALESCE(m.fstype_name, '')) NOT IN ('overlay', 'devtmpfs', 'efivarfs')
            """;

static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(DISTINCT m.mountpoint_pk)
            """ + SOURCE_FROM_AND_FILTER;

static final String SOURCE_QUERY = """
            SELECT DISTINCT ON (m.mountpoint_pk)
                m.mountpoint_pk,
                d.device_pk AS device_fk,
                m.mountpoint,
                m.filesystem,
                m.fstype_name,
                m.capacity,
                m.free_capacity,
                m.label
            """ + SOURCE_FROM_AND_FILTER + """
            ORDER BY m.mountpoint_pk, d.device_pk
            """;
    }
    static final class CpuQuery {
static final String DEVICE_FILTER = """
            d.type IN ('virtual', 'physical')
            AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype NOT IN ('Network Printer', 'PDU')
            )
            """;

static final String SOURCE_FROM_AND_FILTER = """
            FROM view_part_v1 p
            JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
            JOIN view_device_v2 d ON d.device_pk = p.device_fk
            LEFT JOIN view_vendor_v1 v ON v.vendor_pk = pm.vendor_fk
            WHERE pm.type_name = 'CPU'
              AND
            """ + DEVICE_FILTER;

static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            """ + SOURCE_FROM_AND_FILTER;

static final String SOURCE_QUERY = """
            SELECT
                p.part_pk,
                p.device_fk,
                p.slot,
                p.description,
                pm.name AS model_name,
                pm.cores,
                pm.speed,
                pm.speed_unit,
                v.name AS vendor_name
            """ + SOURCE_FROM_AND_FILTER + """
            ORDER BY p.device_fk, p.slot, p.part_pk
            """;
    }
    static final class OsQuery {
static final String DEVICE_FILTER = """
            d.type IN ('virtual', 'physical')
            AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype NOT IN ('Network Printer', 'PDU')
            )
            """;

static final String SOURCE_FROM_AND_FILTER = """
            FROM view_deviceos_v1 o
            JOIN view_device_v2 d ON d.device_pk = o.device_fk
            LEFT JOIN view_os_v1 s ON s.os_pk = o.os_fk
            LEFT JOIN view_vendor_v1 v ON v.vendor_pk = s.vendor_fk
            WHERE
            """ + DEVICE_FILTER;

static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            """ + SOURCE_FROM_AND_FILTER;

static final String SOURCE_QUERY = """
            SELECT
                o.deviceos_pk,
                o.device_fk,
                o.os_name,
                o.os_version,
                o.os_version_no,
                v.name AS vendor_name
            """ + SOURCE_FROM_AND_FILTER + """
            ORDER BY o.device_fk, o.deviceos_pk
            """;
    }
    static final class TcpIpQuery {
static final String DEVICE_FILTER = """
            d.type IN ('virtual', 'physical')
            AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype NOT IN ('Network Printer', 'PDU')
            )
            """;

static final String SOURCE_FROM_AND_FILTER = """
            FROM view_ipaddress_v2 i
            JOIN view_device_v2 d ON d.device_pk = ANY(i.device_fks)
            LEFT JOIN view_subnet_v1 b ON b.subnet_pk = i.subnet_fk
            WHERE
            """ + DEVICE_FILTER;

static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            """ + SOURCE_FROM_AND_FILTER;

static final String SOURCE_QUERY = """
            SELECT
                i.ipaddress_pk,
                d.device_pk AS device_fk,
                d.name AS device_name,
                HOST(i.ip_address) AS ip_address,
                i.ip_hybrid,
                i.label,
                i.subnet_fk,
                i.type_id,
                i.type,
                i.available,
                i.is_public,
                i.resource_fk,
                i.notes,
                i.first_added,
                i.last_edited,
                i.tags,
                i.netport_fk,
                i.details,
                i.last_changed,
                i.last_discovered,
                i.is_shared,
                i.cloudinfrastructure_fk,
                b.gateway,
                b.mask_bits
            """ + SOURCE_FROM_AND_FILTER + """
            ORDER BY i.ipaddress_pk, d.device_pk
            """;
    }
    static final class NetAdapterQuery {
static final String DEVICE_FILTER = """
            d.type IN ('virtual', 'physical')
            AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype NOT IN ('Network Printer', 'PDU')
            )
            """;

static final String SOURCE_FROM_AND_FILTER = """
            FROM view_netport_v1 n
            JOIN view_device_v2 d ON d.device_pk = n.device_fk
            LEFT JOIN view_vendor_v1 v ON v.vendor_pk = n.vendor_fk
            WHERE
            """ + DEVICE_FILTER;

static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            """ + SOURCE_FROM_AND_FILTER;

static final String SOURCE_QUERY = """
            SELECT
                n.netport_pk,
                n.device_fk,
                n.port,
                n.description,
                n.hwaddress,
                n.hwaddress2,
                n.port_speed,
                n.global_type,
                v.name AS vendor_name
            """ + SOURCE_FROM_AND_FILTER + """
            ORDER BY n.device_fk, n.netport_pk
            """;
    }
    static final class DatabaseInstanceCiQuery {
static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*) FROM view_databaseinstance_v2 i
            """;

static final String SOURCE_QUERY = """
            SELECT i.databaseinstance_pk,
                NULLIF(TRIM(i.dbinstance_name), '') AS dbinstance_name,
                NULLIF(TRIM(i.database_type), '') AS database_type,
                NULLIF(TRIM(r.identifier), '') AS resource_identifier,
                NULLIF(TRIM(CAST(r.details AS JSONB)->>'version'), '') AS version_text,
                r.notes AS source_description,
                r.last_changed,
                NULLIF(TRIM(CAST(a.json AS JSONB)->'products'->0->>'install_path'), '') AS install_path
            FROM view_databaseinstance_v2 i
            LEFT JOIN view_resource_v2 r ON r.resource_pk = i.databaseinstance_pk
            LEFT JOIN view_appcomp_v1 a ON a.appcomp_pk = i.appcomp_fk
            ORDER BY i.databaseinstance_pk
            LIMIT %d OFFSET %d
            """;
    }
    static final class DeviceCiQuery {
static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*) FROM view_device_v2 d WHERE
            """ + CiSourceFilter.DEVICE;

static final String SOURCE_QUERY = """
            WITH device AS (
                SELECT d.*
                FROM view_device_v2 d
                WHERE
            """ + CiSourceFilter.DEVICE + """
            ), cpu AS (
                SELECT p.device_fk,
                    COUNT(DISTINCT NULLIF(TRIM(pm.name), '')) AS model_count,
                    MIN(NULLIF(TRIM(pm.name), '')) AS cpu_model,
                    COUNT(DISTINCT NULLIF(TRIM(p.details->>'architecture'), '')) AS arch_count,
                    MIN(NULLIF(TRIM(p.details->>'architecture'), '')) AS architecture
                FROM view_part_v1 p
                JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
                JOIN device d ON d.device_pk = p.device_fk
                WHERE pm.type_name = 'CPU'
                GROUP BY p.device_fk
            ), primary_port AS (
                SELECT n.device_fk, COUNT(*) AS default_port_count,
                    MIN(NULLIF(TRIM(n.hwaddress), '')) AS primary_mac
                FROM view_netport_v1 n
                JOIN device d ON d.device_pk = n.device_fk
                WHERE n.is_default = true
                GROUP BY n.device_fk
            ), network_info AS (
                SELECT n.second_device_fk AS physical_pk,
                    CASE WHEN COUNT(DISTINCT n.device_fk) = 1 THEN MIN(n.device_fk) END AS cluster_pk,
                    COUNT(DISTINCT n.device_fk) AS cluster_count,
                    COUNT(DISTINCT NULLIF(TRIM(c.details->>'fw_device_type'), '')) AS network_kind_count,
                    MIN(NULLIF(TRIM(c.details->>'fw_device_type'), '')) AS network_kind,
                    MIN(NULLIF(TRIM(n.hwaddress), '')) AS network_mac
                FROM view_netport_v1 n
                JOIN device d ON d.device_pk = n.second_device_fk
                JOIN view_device_v2 c ON c.device_pk = n.device_fk
                WHERE d.type = 'physical' AND d.network_device = true
                  AND c.type = 'cluster' AND c.network_device = true
                GROUP BY n.second_device_fk
            )
            SELECT d.device_pk, d.type, d.physicalsubtype, d.network_device,
                ni.cluster_pk,
                CASE WHEN d.type = 'cluster'
                     THEN NULLIF(TRIM(d.details->>'fw_device_type'), '')
                     ELSE ni.network_kind END AS network_kind,
                CASE WHEN d.type = 'cluster'
                          AND NULLIF(TRIM(d.details->>'fw_device_type'), '') IS NOT NULL
                     THEN 1 ELSE ni.network_kind_count END AS network_kind_count,
                ni.cluster_count,
                NULLIF(TRIM(d.details->>'snmp_location'), '') AS snmp_location,
                'D42:DEVICE:' || CAST(d.device_pk AS varchar) AS source_id,
                d.name, d.notes, d.serial_no, d.uuid, d.last_discovered,
                h.name AS model, v.name AS manufacturer,
                d.ram, d.ram_size_type, d.total_cpus, d.core_per_cpu,
                CAST(d.total_cpus AS bigint) * d.core_per_cpu AS total_cores,
                d.cpu_speed, d.hz AS cpu_speed_unit,
                CASE WHEN cpu.model_count = 1 THEN cpu.cpu_model END AS cpu_type,
                CASE WHEN cpu.arch_count = 1 THEN cpu.architecture END AS architecture,
                CASE WHEN d.network_device = true THEN ni.network_mac
                     WHEN pp.default_port_count = 1 THEN pp.primary_mac END AS primary_mac,
                'ComputerSystem' AS system_type,
                CASE d.type WHEN 'virtual' THEN 'true' ELSE 'false' END AS is_virtual,
                CASE WHEN d.type = 'virtual' THEN d.vm_manager_int_id END AS vm_id,
                b.name AS bios_manufacturer, d.bios_version, d.bios_release_date,
                cpu.model_count, cpu.arch_count, pp.default_port_count
            FROM device d
            LEFT JOIN view_hardware_v2 h ON h.hardware_pk = d.hardware_fk
            LEFT JOIN view_vendor_v1 v ON v.vendor_pk = h.vendor_fk
            LEFT JOIN view_vendor_v1 b ON b.vendor_pk = d.bios_vendor_fk
            LEFT JOIN cpu ON cpu.device_fk = d.device_pk
            LEFT JOIN primary_port pp ON pp.device_fk = d.device_pk
            LEFT JOIN network_info ni ON ni.physical_pk = d.device_pk
            ORDER BY d.device_pk
            LIMIT %d OFFSET %d
            """;
    }
    static final class IpCiQuery {
static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            FROM view_ipaddress_v2 i
            WHERE EXISTS (
                SELECT 1 FROM view_device_v2 d WHERE d.device_pk = ANY(i.device_fks)
            )
            """;

static final String SOURCE_QUERY = """
            SELECT DISTINCT ON (i.ipaddress_pk)
                i.ipaddress_pk, d.device_pk AS device_fk,
                HOST(i.ip_address) AS ip_address,
                NULLIF(TRIM(d.name), '') AS device_name,
                NULLIF(TRIM(i.label), '') AS label,
                NULLIF(TRIM(i.notes), '') AS notes,
                i.last_discovered
            FROM view_ipaddress_v2 i
            JOIN view_device_v2 d ON d.device_pk = ANY(i.device_fks)
            ORDER BY i.ipaddress_pk, d.device_pk
            LIMIT %d OFFSET %d
            """;
    }
    static final class DiskCiQuery {
static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            FROM view_part_v1 p
            JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
            JOIN view_device_v2 d ON d.device_pk = p.device_fk
            WHERE pm.type_name = 'Hard Disk' AND
            """ + CiSourceFilter.COMPUTER;

static final String SOURCE_QUERY = """
            WITH computer AS (
                SELECT d.device_pk, d.last_discovered
                FROM view_device_v2 d
                WHERE
            """ + CiSourceFilter.COMPUTER + """
            )
            SELECT p.part_pk, p.device_fk,
                NULLIF(TRIM(pm.name), '') AS model,
                NULLIF(TRIM(p.serial_no), '') AS serial_no,
                NULLIF(TRIM(p.description), '') AS description,
                pm.hdsize, NULLIF(TRIM(pm.hdsize_unit), '') AS hdsize_unit,
                c.last_discovered
            FROM view_part_v1 p
            JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
            JOIN computer c ON c.device_pk = p.device_fk
            WHERE pm.type_name = 'Hard Disk'
            ORDER BY p.part_pk
            LIMIT %d OFFSET %d
            """;
    }
    static final class FilesystemCiQuery {
static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            FROM view_mountpoint_v2 m
            WHERE (m.fstype_name IS NULL OR m.fstype_name NOT IN (""" + FilesystemSelection.EXCLUDED_TYPES_SQL + """
            ))
            AND EXISTS (
                SELECT 1 FROM view_device_v2 d
                WHERE d.device_pk = ANY(m.device_fks) AND
            """ + CiSourceFilter.COMPUTER + """
            )
            """;

static final String SOURCE_QUERY = """
            WITH computer AS (
                SELECT d.device_pk, d.last_discovered
                FROM view_device_v2 d
                WHERE
            """ + CiSourceFilter.COMPUTER + """
            )
            SELECT DISTINCT ON (m.mountpoint_pk)
                m.mountpoint_pk, c.device_pk AS device_fk,
                NULLIF(TRIM(m.mountpoint), '') AS mountpoint,
                NULLIF(TRIM(m.fstype_name), '') AS fstype_name,
                NULLIF(TRIM(m.label), '') AS label,
                m.capacity, m.free_capacity, c.last_discovered
            FROM view_mountpoint_v2 m
            JOIN computer c ON c.device_pk = ANY(m.device_fks)
            WHERE (m.fstype_name IS NULL OR m.fstype_name NOT IN (""" + FilesystemSelection.EXCLUDED_TYPES_SQL + """
            ))
            ORDER BY m.mountpoint_pk, c.device_pk
            LIMIT %d OFFSET %d
            """;
    }
    static final class OsCiQuery {
static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            FROM view_deviceos_v1 o
            JOIN view_device_v2 d ON d.device_pk = o.device_fk
            WHERE
            """ + CiSourceFilter.COMPUTER;

static final String SOURCE_QUERY = """
            WITH computer AS (
                SELECT d.device_pk, d.last_discovered
                FROM view_device_v2 d
                WHERE
            """ + CiSourceFilter.COMPUTER + """
            )
            SELECT o.deviceos_pk, o.device_fk,
                NULLIF(TRIM(o.os_name), '') AS os_name,
                NULLIF(TRIM(o.os_version), '') AS os_version,
                NULLIF(TRIM(o.os_version_no), '') AS os_version_no,
                NULLIF(TRIM(o.os_arch_name), '') AS os_arch_name,
                c.last_discovered
            FROM view_deviceos_v1 o
            JOIN computer c ON c.device_pk = o.device_fk
            ORDER BY o.deviceos_pk
            LIMIT %d OFFSET %d
            """;
    }
    static final class TloamSoftwareQuery {
static final String DEVICE_FILTER = """
            d.type IN ('virtual', 'physical')
            AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype NOT IN ('Network Printer', 'PDU')
            )
            """;

static final String CATALOG_SOURCE_QUERY = """
            SELECT DISTINCT
                COALESCE(NULLIF(TRIM(s.name), ''), 'UNKNOWN') AS software_name,
                NULLIF(TRIM(u.version), '') AS version,
                COALESCE(NULLIF(TRIM(v.name), ''), 'UNKNOWN') AS manufacturer
            FROM view_softwareinuse_v1 u
            JOIN view_device_v2 d ON d.device_pk = u.device_fk
            LEFT JOIN view_software_v1 s ON s.software_pk = u.software_fk
            LEFT JOIN view_vendor_v1 v ON v.vendor_pk = s.vendor_fk
            WHERE
            """ + DEVICE_FILTER;

static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            FROM (
            """ + CATALOG_SOURCE_QUERY + """
            ) catalog
            """;

static final String SOURCE_QUERY = CATALOG_SOURCE_QUERY + """
            ORDER BY software_name, version, manufacturer
            """;
    }
    static final class DpaSoftwareQuery {
static final String DEVICE_FILTER = """
            d.type IN ('virtual', 'physical')
            AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype NOT IN ('Network Printer', 'PDU')
            )
            """;

static final String SOURCE_FROM_AND_FILTER = """
            FROM view_softwareinuse_v1 u
            JOIN view_device_v2 d ON d.device_pk = u.device_fk
            LEFT JOIN view_software_v1 s ON s.software_pk = u.software_fk
            LEFT JOIN view_vendor_v1 v ON v.vendor_pk = s.vendor_fk
            WHERE
            """ + DEVICE_FILTER;

static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            """ + SOURCE_FROM_AND_FILTER;

static final String SOURCE_QUERY = """
            SELECT
                u.softwareinuse_pk,
                u.device_fk,
                s.name AS software_name,
                u.version,
                u.install_path,
                u.install_date,
                u.first_detected,
                u.last_updated,
                v.name AS vendor_name
            """ + SOURCE_FROM_AND_FILTER + """
            ORDER BY u.device_fk, s.name, u.version, u.softwareinuse_pk
            """;
    }
/** 모든 CI 유형이 같은 Computer 집합을 부모로 삼도록 조건을 한 곳에 둔다. 별칭은 d다. */
static final class CiSourceFilter {
    private CiSourceFilter() {
    }

    public static final String COMPUTER = """
            d.type IN ('physical', 'virtual')
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                (d.type = 'physical' AND d.physicalsubtype IN
                    ('Generic', 'Rackable', 'Blade', 'WorkStation', 'ThinClient', 'Laptop'))
                OR
                (d.type = 'virtual' AND d.virtualsubtype IN
                    ('Internal VM', 'Amazon EC2 Instance', 'VMWare', 'Hyper-V'))
            )
            """;

    /** Device 본체 수집 후보. 미판별 네트워크 장비도 조회해 매핑 단계에서 진단한다. */
    public static final String DEVICE = """
            (
            """ + COMPUTER + """
            )
            OR (
                d.type = 'physical'
                AND d.network_device = true
            )
            OR (
                d.type = 'cluster'
                AND d.network_device = true
                AND NULLIF(TRIM(d.details->>'fw_device_type'), '') = 'Switch'
            )
            """;
}



static final class FilesystemSelection {
    private FilesystemSelection() {}

    /**
     * 적재하지 않는 파일시스템 종류. 컨테이너 런타임·이미지 마운트다.
     * 경로에 컨테이너 ID가 들어가 재기동 시 원천 PK가 바뀌면 매 실행마다 새 CI가 쌓인다.
     * devtmpfs는 경로가 고정돼 이 문제가 없어 수집 대상이다.
     */
    public static final List<String> EXCLUDED_TYPES = List.of("overlay", "squashfs", "efivarfs");

    /** EXCLUDED_TYPES를 DOQL IN 절에 넣을 수 있게 join한 문자열. CiRelationSource도 재사용한다. */
    public static final String EXCLUDED_TYPES_SQL = EXCLUDED_TYPES.stream()
            .map(type -> "'" + type + "'").collect(Collectors.joining(", "));
}


/**
 * 관계 하나의 정의. 관계를 늘릴 때 늘어나는 것은 이 enum의 상수 하나뿐이다.
 * 조회 반복·DTO 변환·저장·집계는 CiRelationJob이 공유한다.
 * 페이지 SQL은 sourceci·targetci 두 컬럼만 돌려주고 관계 키로 정렬한다.
 * DOQL은 정렬 없는 OFFSET의 순서를 보장하지 않는다.
 */
enum CiRelationSource {
    /** OS → Computer. 근거: view_deviceos_v1 의 deviceos_pk·device_fk. */
    OS_INSTALLED_ON_COMPUTER("RELATION.INSTALLEDON", Queries.OS_COUNT, Queries.OS_PAGE),

    /** Computer → Disk. 근거: Hard Disk 조건의 part_pk·device_fk. */
    COMPUTER_CONTAINS_DISK("RELATION.CONTAINS", Queries.DISK_COUNT, Queries.DISK_PAGE),

    /** Computer → Filesystem. 근거: mountpoint_pk·device_fks. 배열의 모든 연결을 보존한다. */
    COMPUTER_CONTAINS_FILESYSTEM("RELATION.CONTAINS", Queries.FILESYSTEM_COUNT, Queries.FILESYSTEM_PAGE),

    /** Virtual Host → VM. 근거: VM 행의 device_pk·virtual_host_device_fk. */
    HOST_VIRTUALIZES_VM("VIRTUALIZES", Queries.HOST_VM_COUNT, Queries.HOST_VM_PAGE),

    /**
     * DB Instance → Device. 근거: databaseinstance.appcomp_fk → appcomp.device_fk.
     * 경유하는 Application Component는 관계 노드로 만들지 않는다.
     * 엔진별 네 분류가 모두 같은 규칙을 가지므로 출발 분류로 분기하지 않는다.
     */
    DB_INSTANCE_RUNS_ON_DEVICE("RELATION.RUNSON", Queries.DB_INSTANCE_COUNT, Queries.DB_INSTANCE_PAGE),

    /**
     * Device → IP. 근거: view_ipaddress_device_v2 의 device_fk·ipaddress_fk.
     * 접두어 없는 USES 는 CDM 표준 밖의 로컬 확장이다. 표준 경로는 IPINTERFACE 를 경유하지만
     * 원천에 그 계층이 없고 netport_fk 가 없는 IP 가 10~16% 라 연결이 줄어든다.
     * 사유와 대조는 design/ci/relations.md 의 IP 절에 있다.
     */
    DEVICE_USES_IP("USES", Queries.DEVICE_IP_COUNT, Queries.DEVICE_IP_PAGE),

    /** Network Cluster → 물리 Network Device. 포트 반복은 물리 장비별 집계로 한 쌍만 남긴다. */
    NETWORK_CLUSTER_FEDERATES_DEVICE(
            "FEDERATES", Queries.NETWORK_CLUSTER_COUNT, Queries.NETWORK_CLUSTER_PAGE);

    private final String relationNum;
    private final String countQuery;
    private final String pageQuery;

    CiRelationSource(String relationNum, String countQuery, String pageQuery) {
        this.relationNum = relationNum;
        this.countQuery = countQuery;
        this.pageQuery = pageQuery;
    }

    public String relationNum() {
        return relationNum;
    }

    public String countQuery() {
        return countQuery;
    }

    public String pageQuery(long offset, int limit) {
        // formatted 인자 순서는 파라미터 순서(offset, limit)와 반대다: %d는 SQL의 LIMIT, OFFSET 순서.
        return pageQuery.formatted(limit, offset);
    }

    /** DOQL이 FROM 서브쿼리를 막아 건수 SQL과 페이지 SQL을 따로 둔다. */
    private static final class Queries {
        static final String OS_COUNT = """
                SELECT COUNT(*)
                FROM view_deviceos_v1 o
                JOIN view_device_v2 d ON d.device_pk = o.device_fk
                WHERE
                """ + CiSourceFilter.COMPUTER;

        static final String OS_PAGE = """
                WITH computer AS (
                    SELECT d.device_pk
                    FROM view_device_v2 d
                    WHERE
                """ + CiSourceFilter.COMPUTER + """
                )
                SELECT 'D42:DEVICEOS:' || CAST(o.deviceos_pk AS varchar) AS sourceci,
                       'D42:DEVICE:' || CAST(c.device_pk AS varchar) AS targetci
                FROM view_deviceos_v1 o
                JOIN computer c ON c.device_pk = o.device_fk
                ORDER BY sourceci, targetci
                LIMIT %d OFFSET %d
                """;

        static final String DISK_COUNT = """
                SELECT COUNT(*)
                FROM view_part_v1 p
                JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
                JOIN view_device_v2 d ON d.device_pk = p.device_fk
                WHERE pm.type_name = 'Hard Disk' AND
                """ + CiSourceFilter.COMPUTER;

        static final String DISK_PAGE = """
                WITH computer AS (
                    SELECT d.device_pk
                    FROM view_device_v2 d
                    WHERE
                """ + CiSourceFilter.COMPUTER + """
                )
                SELECT 'D42:DEVICE:' || CAST(c.device_pk AS varchar) AS sourceci,
                       'D42:PART:' || CAST(p.part_pk AS varchar) AS targetci
                FROM view_part_v1 p
                JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
                JOIN computer c ON c.device_pk = p.device_fk
                WHERE pm.type_name = 'Hard Disk'
                ORDER BY sourceci, targetci
                LIMIT %d OFFSET %d
                """;

        /** 본체 건수는 마운트포인트를 세지만 관계 건수는 장비 배열을 펼친 쌍을 센다. */
        static final String FILESYSTEM_COUNT = """
                SELECT COUNT(*)
                FROM view_mountpoint_v2 m
                JOIN view_device_v2 d ON d.device_pk = ANY(m.device_fks)
                WHERE (m.fstype_name IS NULL OR m.fstype_name NOT IN (""" + FilesystemSelection.EXCLUDED_TYPES_SQL + """
                ))
                AND
                """ + CiSourceFilter.COMPUTER;

        static final String FILESYSTEM_PAGE = """
                WITH computer AS (
                    SELECT d.device_pk
                    FROM view_device_v2 d
                    WHERE
                """ + CiSourceFilter.COMPUTER + """
                )
                SELECT 'D42:DEVICE:' || CAST(c.device_pk AS varchar) AS sourceci,
                       'D42:MOUNTPOINT:' || CAST(m.mountpoint_pk AS varchar) AS targetci
                FROM view_mountpoint_v2 m
                JOIN computer c ON c.device_pk = ANY(m.device_fks)
                WHERE (m.fstype_name IS NULL OR m.fstype_name NOT IN (""" + FilesystemSelection.EXCLUDED_TYPES_SQL + """
                ))
                ORDER BY sourceci, targetci
                LIMIT %d OFFSET %d
                """;

        static final String HOST_VM_COUNT = """
                WITH computer AS (
                    SELECT d.device_pk, d.type, d.virtual_host_device_fk
                    FROM view_device_v2 d
                    WHERE
                """ + CiSourceFilter.COMPUTER + """
                )
                SELECT COUNT(*)
                FROM computer vm
                JOIN computer host ON host.device_pk = vm.virtual_host_device_fk
                WHERE vm.type = 'virtual'
                  AND host.device_pk <> vm.device_pk
                """;

        static final String HOST_VM_PAGE = """
                WITH computer AS (
                    SELECT d.device_pk, d.type, d.virtual_host_device_fk
                    FROM view_device_v2 d
                    WHERE
                """ + CiSourceFilter.COMPUTER + """
                )
                SELECT 'D42:DEVICE:' || CAST(host.device_pk AS varchar) AS sourceci,
                       'D42:DEVICE:' || CAST(vm.device_pk AS varchar) AS targetci
                FROM computer vm
                JOIN computer host ON host.device_pk = vm.virtual_host_device_fk
                WHERE vm.type = 'virtual'
                  AND host.device_pk <> vm.device_pk
                ORDER BY sourceci, targetci
                LIMIT %d OFFSET %d
                """;

        static final String DB_INSTANCE_COUNT = """
                SELECT COUNT(*)
                FROM view_databaseinstance_v2 i
                JOIN view_appcomp_v1 a ON a.appcomp_pk = i.appcomp_fk
                JOIN view_device_v2 d ON d.device_pk = a.device_fk
                WHERE
                """ + CiSourceFilter.COMPUTER;

        static final String DB_INSTANCE_PAGE = """
                WITH computer AS (
                    SELECT d.device_pk
                    FROM view_device_v2 d
                    WHERE
                """ + CiSourceFilter.COMPUTER + """
                )
                SELECT 'D42:DATABASEINSTANCE:' || CAST(i.databaseinstance_pk AS varchar) AS sourceci,
                       'D42:DEVICE:' || CAST(c.device_pk AS varchar) AS targetci
                FROM view_databaseinstance_v2 i
                JOIN view_appcomp_v1 a ON a.appcomp_pk = i.appcomp_fk
                JOIN computer c ON c.device_pk = a.device_fk
                ORDER BY sourceci, targetci
                LIMIT %d OFFSET %d
                """;

        /** 장비-IP 연결은 배열 전개 대신 전용 연결 뷰를 쓴다. 같은 쌍 수를 돌려준다. */
        static final String DEVICE_IP_COUNT = """
                SELECT COUNT(*)
                FROM view_ipaddress_device_v2 x
                JOIN view_device_v2 d ON d.device_pk = x.device_fk
                WHERE
                """ + CiSourceFilter.DEVICE;

        static final String DEVICE_IP_PAGE = """
                WITH device AS (
                    SELECT d.device_pk
                    FROM view_device_v2 d
                    WHERE
                """ + CiSourceFilter.DEVICE + """
                )
                SELECT 'D42:DEVICE:' || CAST(d.device_pk AS varchar) AS sourceci,
                       'D42:IPADDRESS:' || CAST(x.ipaddress_fk AS varchar) AS targetci
                FROM view_ipaddress_device_v2 x
                JOIN device d ON d.device_pk = x.device_fk
                ORDER BY sourceci, targetci
                LIMIT %d OFFSET %d
                """;

        static final String NETWORK_CLUSTER_COUNT = """
                WITH network_info AS (
                    SELECT n.second_device_fk AS physical_pk,
                           CASE WHEN COUNT(DISTINCT n.device_fk) = 1
                                THEN MIN(n.device_fk) END AS cluster_pk,
                           COUNT(DISTINCT n.device_fk) AS cluster_count,
                           COUNT(DISTINCT NULLIF(TRIM(c.details->>'fw_device_type'), ''))
                               AS network_kind_count,
                           MIN(NULLIF(TRIM(c.details->>'fw_device_type'), '')) AS network_kind
                    FROM view_netport_v1 n
                    JOIN view_device_v2 p ON p.device_pk = n.second_device_fk
                    JOIN view_device_v2 c ON c.device_pk = n.device_fk
                    WHERE p.type = 'physical' AND p.network_device = true
                      AND (p.physicalsubtype IS NULL OR p.physicalsubtype <> 'Network Printer')
                      AND c.type = 'cluster' AND c.network_device = true
                    GROUP BY n.second_device_fk
                )
                SELECT COUNT(*)
                FROM network_info
                WHERE cluster_count = 1
                  AND network_kind_count = 1
                  AND network_kind = 'Switch'
                """;

        static final String NETWORK_CLUSTER_PAGE = """
                WITH network_info AS (
                    SELECT n.second_device_fk AS physical_pk,
                           CASE WHEN COUNT(DISTINCT n.device_fk) = 1
                                THEN MIN(n.device_fk) END AS cluster_pk,
                           COUNT(DISTINCT n.device_fk) AS cluster_count,
                           COUNT(DISTINCT NULLIF(TRIM(c.details->>'fw_device_type'), ''))
                               AS network_kind_count,
                           MIN(NULLIF(TRIM(c.details->>'fw_device_type'), '')) AS network_kind
                    FROM view_netport_v1 n
                    JOIN view_device_v2 p ON p.device_pk = n.second_device_fk
                    JOIN view_device_v2 c ON c.device_pk = n.device_fk
                    WHERE p.type = 'physical' AND p.network_device = true
                      AND (p.physicalsubtype IS NULL OR p.physicalsubtype <> 'Network Printer')
                      AND c.type = 'cluster' AND c.network_device = true
                    GROUP BY n.second_device_fk
                )
                SELECT 'D42:DEVICE:' || CAST(cluster_pk AS varchar) AS sourceci,
                       'D42:DEVICE:' || CAST(physical_pk AS varchar) AS targetci
                FROM network_info
                WHERE cluster_count = 1
                  AND network_kind_count = 1
                  AND network_kind = 'Switch'
                ORDER BY sourceci, targetci
                LIMIT %d OFFSET %d
                """;
    }
}


}
