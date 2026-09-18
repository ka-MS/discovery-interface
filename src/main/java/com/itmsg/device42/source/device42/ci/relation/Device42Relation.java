package com.itmsg.device42.source.device42.ci.relation;

import com.itmsg.device42.source.device42.selection.DeviceSelection;
import com.itmsg.device42.source.device42.selection.FilesystemFilter;

/** D42 뷰의 연결 사실. PK는 문자열로 정렬하여 기존 페이지 경계를 유지한다. */
public enum Device42Relation {
    OS_DEVICE(Device42Entity.DEVICEOS, Device42Entity.DEVICE, Queries.OS_COUNT, Queries.OS_PAGE),
    DEVICE_DISK(Device42Entity.DEVICE, Device42Entity.PART, Queries.DISK_COUNT, Queries.DISK_PAGE),
    DEVICE_FILESYSTEM(Device42Entity.DEVICE, Device42Entity.MOUNTPOINT, Queries.FILESYSTEM_COUNT, Queries.FILESYSTEM_PAGE),
    HOST_VM(Device42Entity.DEVICE, Device42Entity.DEVICE, Queries.HOST_VM_COUNT, Queries.HOST_VM_PAGE),
    DATABASE_INSTANCE_DEVICE(Device42Entity.DATABASEINSTANCE, Device42Entity.DEVICE, Queries.DB_INSTANCE_COUNT, Queries.DB_INSTANCE_PAGE),
    DEVICE_IP(Device42Entity.DEVICE, Device42Entity.IPADDRESS, Queries.DEVICE_IP_COUNT, Queries.DEVICE_IP_PAGE),
    NETWORK_CLUSTER_DEVICE(Device42Entity.DEVICE, Device42Entity.DEVICE, Queries.NETWORK_CLUSTER_COUNT, Queries.NETWORK_CLUSTER_PAGE);

    private final Device42Entity from;
    private final Device42Entity to;
    private final String count;
    private final String page;

    Device42Relation(Device42Entity from, Device42Entity to, String count, String page) {
        this.from = from;
        this.to = to;
        this.count = count;
        this.page = page;
    }

    public Device42Entity from() { return from; }
    public Device42Entity to() { return to; }

    public String countQuery(Selection selection) { return selection.apply(count); }
    public String pageQuery(Selection selection, long offset, int limit) {
        return selection.apply(page).formatted(limit, offset);
    }

    public record Selection(DeviceSelection computer, DeviceSelection device, FilesystemFilter filesystem,
                            String networkKind, String excludedPhysicalSubtype) {
        private String apply(String sql) {
            return sql.replace("{{COMPUTER}}", computer.sql())
                    .replace("{{DEVICE}}", device.sql())
                    .replace("{{FILESYSTEM}}", filesystem.sql(false))
                    .replace("{{NETWORK_KIND}}", DeviceSelection.literal(networkKind))
                    .replace("{{EXCLUDED_SUBTYPE}}", DeviceSelection.literal(excludedPhysicalSubtype));
        }
    }

    /** DOQL이 FROM 서브쿼리를 막아 건수 SQL과 페이지 SQL을 따로 둔다. */
    private static final class Queries {
        static final String OS_COUNT = """
                SELECT COUNT(*)
                FROM view_deviceos_v1 o
                JOIN view_device_v2 d ON d.device_pk = o.device_fk
                WHERE
                """ + "{{COMPUTER}}";

        static final String OS_PAGE = """
                WITH computer AS (
                    SELECT d.device_pk
                    FROM view_device_v2 d
                    WHERE
                """ + "{{COMPUTER}}" + """
                )
                SELECT CAST(o.deviceos_pk AS varchar) AS source_pk,
                       CAST(c.device_pk AS varchar) AS target_pk
                FROM view_deviceos_v1 o
                JOIN computer c ON c.device_pk = o.device_fk
                ORDER BY source_pk, target_pk
                LIMIT %d OFFSET %d
                """;

        static final String DISK_COUNT = """
                SELECT COUNT(*)
                FROM view_part_v1 p
                JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
                JOIN view_device_v2 d ON d.device_pk = p.device_fk
                WHERE pm.type_name = 'Hard Disk' AND
                """ + "{{COMPUTER}}";

        static final String DISK_PAGE = """
                WITH computer AS (
                    SELECT d.device_pk
                    FROM view_device_v2 d
                    WHERE
                """ + "{{COMPUTER}}" + """
                )
                SELECT CAST(c.device_pk AS varchar) AS source_pk,
                       CAST(p.part_pk AS varchar) AS target_pk
                FROM view_part_v1 p
                JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
                JOIN computer c ON c.device_pk = p.device_fk
                WHERE pm.type_name = 'Hard Disk'
                ORDER BY source_pk, target_pk
                LIMIT %d OFFSET %d
                """;

        /** 본체 건수는 마운트포인트를 세지만 관계 건수는 장비 배열을 펼친 쌍을 센다. */
        static final String FILESYSTEM_COUNT = """
                SELECT COUNT(*)
                FROM view_mountpoint_v2 m
                JOIN view_device_v2 d ON d.device_pk = ANY(m.device_fks)
                WHERE {{FILESYSTEM}}
                AND
                """ + "{{COMPUTER}}";

        static final String FILESYSTEM_PAGE = """
                WITH computer AS (
                    SELECT d.device_pk
                    FROM view_device_v2 d
                    WHERE
                """ + "{{COMPUTER}}" + """
                )
                SELECT CAST(c.device_pk AS varchar) AS source_pk,
                       CAST(m.mountpoint_pk AS varchar) AS target_pk
                FROM view_mountpoint_v2 m
                JOIN computer c ON c.device_pk = ANY(m.device_fks)
                WHERE {{FILESYSTEM}}
                ORDER BY source_pk, target_pk
                LIMIT %d OFFSET %d
                """;

        static final String HOST_VM_COUNT = """
                WITH computer AS (
                    SELECT d.device_pk, d.type, d.virtual_host_device_fk
                    FROM view_device_v2 d
                    WHERE
                """ + "{{COMPUTER}}" + """
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
                """ + "{{COMPUTER}}" + """
                )
                SELECT CAST(host.device_pk AS varchar) AS source_pk,
                       CAST(vm.device_pk AS varchar) AS target_pk
                FROM computer vm
                JOIN computer host ON host.device_pk = vm.virtual_host_device_fk
                WHERE vm.type = 'virtual'
                  AND host.device_pk <> vm.device_pk
                ORDER BY source_pk, target_pk
                LIMIT %d OFFSET %d
                """;

        static final String DB_INSTANCE_COUNT = """
                SELECT COUNT(*)
                FROM view_databaseinstance_v2 i
                JOIN view_appcomp_v1 a ON a.appcomp_pk = i.appcomp_fk
                JOIN view_device_v2 d ON d.device_pk = a.device_fk
                WHERE
                """ + "{{COMPUTER}}";

        static final String DB_INSTANCE_PAGE = """
                WITH computer AS (
                    SELECT d.device_pk
                    FROM view_device_v2 d
                    WHERE
                """ + "{{COMPUTER}}" + """
                )
                SELECT CAST(i.databaseinstance_pk AS varchar) AS source_pk,
                       CAST(c.device_pk AS varchar) AS target_pk
                FROM view_databaseinstance_v2 i
                JOIN view_appcomp_v1 a ON a.appcomp_pk = i.appcomp_fk
                JOIN computer c ON c.device_pk = a.device_fk
                ORDER BY source_pk, target_pk
                LIMIT %d OFFSET %d
                """;

        /** 장비-IP 연결은 배열 전개 대신 전용 연결 뷰를 쓴다. 같은 쌍 수를 돌려준다. */
        static final String DEVICE_IP_COUNT = """
                SELECT COUNT(*)
                FROM view_ipaddress_device_v2 x
                JOIN view_device_v2 d ON d.device_pk = x.device_fk
                WHERE
                """ + "{{DEVICE}}";

        static final String DEVICE_IP_PAGE = """
                WITH device AS (
                    SELECT d.device_pk
                    FROM view_device_v2 d
                    WHERE
                """ + "{{DEVICE}}" + """
                )
                SELECT CAST(d.device_pk AS varchar) AS source_pk,
                       CAST(x.ipaddress_fk AS varchar) AS target_pk
                FROM view_ipaddress_device_v2 x
                JOIN device d ON d.device_pk = x.device_fk
                ORDER BY source_pk, target_pk
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
                      AND (p.physicalsubtype IS NULL OR p.physicalsubtype <> {{EXCLUDED_SUBTYPE}})
                      AND c.type = 'cluster' AND c.network_device = true
                    GROUP BY n.second_device_fk
                )
                SELECT COUNT(*)
                FROM network_info
                WHERE cluster_count = 1
                  AND network_kind_count = 1
                  AND network_kind = {{NETWORK_KIND}}
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
                      AND (p.physicalsubtype IS NULL OR p.physicalsubtype <> {{EXCLUDED_SUBTYPE}})
                      AND c.type = 'cluster' AND c.network_device = true
                    GROUP BY n.second_device_fk
                )
                SELECT CAST(cluster_pk AS varchar) AS source_pk,
                       CAST(physical_pk AS varchar) AS target_pk
                FROM network_info
                WHERE cluster_count = 1
                  AND network_kind_count = 1
                  AND network_kind = {{NETWORK_KIND}}
                ORDER BY source_pk, target_pk
                LIMIT %d OFFSET %d
                """;
    }
}
