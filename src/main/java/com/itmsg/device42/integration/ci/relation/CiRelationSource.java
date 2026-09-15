package com.itmsg.device42.integration.ci.relation;

import com.itmsg.device42.integration.ci.CiSourceFilter;

/**
 * 관계 하나의 정의. 관계를 늘릴 때 늘어나는 것은 이 enum의 상수 하나뿐이다.
 * 조회 반복·DTO 변환·저장·집계는 CiRelationJob이 공유한다.
 * 페이지 SQL은 sourceci·targetci 두 컬럼만 돌려주고 관계 키로 정렬한다.
 * DOQL은 정렬 없는 OFFSET의 순서를 보장하지 않는다.
 */
public enum CiRelationSource {
    /** OS → Computer. 근거: view_deviceos_v1 의 deviceos_pk·device_fk. */
    OS_INSTALLED_ON_COMPUTER("RELATION.INSTALLEDON", Queries.OS_COUNT, Queries.OS_PAGE);

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
    }
}
