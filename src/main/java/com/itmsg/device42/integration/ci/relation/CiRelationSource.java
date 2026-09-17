package com.itmsg.device42.integration.ci.relation;

import com.itmsg.device42.integration.ci.CiSourceFilter;
import com.itmsg.device42.integration.d42maximo.ci.FilesystemSelection;

/**
 * 관계 하나의 정의. 관계를 늘릴 때 늘어나는 것은 이 enum의 상수 하나뿐이다.
 * 조회 반복·DTO 변환·저장·집계는 CiRelationJob이 공유한다.
 * 페이지 SQL은 sourceci·targetci 두 컬럼만 돌려주고 관계 키로 정렬한다.
 * DOQL은 정렬 없는 OFFSET의 순서를 보장하지 않는다.
 */
public enum CiRelationSource {
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
