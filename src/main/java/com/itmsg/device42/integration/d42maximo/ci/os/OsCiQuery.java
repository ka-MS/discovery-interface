package com.itmsg.device42.integration.d42maximo.ci.os;

import com.itmsg.device42.device42.DoqlClient;
import com.itmsg.device42.integration.d42maximo.ci.selection.CiSourceFilter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OsCiQuery {

    private static final Logger log = LoggerFactory.getLogger(OsCiQuery.class);

    private final DoqlClient doql;

    public OsCiQuery(DoqlClient doql) {
        this.doql = doql;
    }

    public long getTotalCount() {
        try {
            return doql.query(TOTAL_COUNT_QUERY, rs -> {
                return rs.next() ? rs.getLong(1) : 0L;
            });
        } catch (SQLException e) {
            throw new IllegalStateException("OS 건수 조회에 실패했습니다.", e);
        }
    }

    public List<OsSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY.formatted(limit, offset);
        try {
            return doql.query(query, rs -> {
                List<OsSource> data = new ArrayList<>(limit);
                while (rs.next()) {
                    long deviceOsPk = rs.getLong("deviceos_pk");
                    try {
                        data.add(new OsSource(
                                deviceOsPk, rs.getLong("device_fk"), rs.getString("os_name"),
                                rs.getString("os_version"), rs.getString("os_version_no"),
                                rs.getString("os_arch_name"), rs.getString("last_discovered")));
                    } catch (SQLException e) {
                        log.error("OS 원천 변환에 실패했습니다. deviceOsPk={}", deviceOsPk, e);
                    }
                }
                return data;
            });
        } catch (SQLException e) {
            throw new IllegalStateException("OS 조회에 실패했습니다. offset=" + offset, e);
        }
    }

    private static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            FROM view_deviceos_v1 o
            JOIN view_device_v2 d ON d.device_pk = o.device_fk
            WHERE
            """ + CiSourceFilter.COMPUTER;

    private static final String SOURCE_QUERY = """
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
