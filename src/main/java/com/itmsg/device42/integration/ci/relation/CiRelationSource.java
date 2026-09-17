package com.itmsg.device42.integration.ci.relation;

import com.itmsg.device42.integration.ci.CiSourceFilter;
import com.itmsg.device42.integration.ci.FilesystemCiIntegrate;

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
    DB_INSTANCE_RUNS_ON_DEVICE("RELATION.RUNSON", Queries.DB_INSTANCE_COUNT, Queries.DB_INSTANCE_PAGE);

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
                WHERE (m.fstype_name IS NULL OR m.fstype_name NOT IN (""" + FilesystemCiIntegrate.EXCLUDED_TYPES_SQL + """
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
                WHERE (m.fstype_name IS NULL OR m.fstype_name NOT IN (""" + FilesystemCiIntegrate.EXCLUDED_TYPES_SQL + """
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
    }
}
