package com.itmsg.device42.integration.d42maximo.software.catalog;

import com.itmsg.device42.device42.DoqlClient;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TloamSoftwareQuery {

    private final DoqlClient doql;

    public TloamSoftwareQuery(DoqlClient doql) {
        this.doql = doql;
    }

    public long getTotalCount() {
        try {
            return doql.preparedQuery(TOTAL_COUNT_QUERY, resultSet -> {

                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }

                return 0L;
            });
        } catch (SQLException e) {
            throw new IllegalStateException("TLOAM 소프트웨어 카탈로그 대상 건수 조회에 실패했습니다.", e);
        }
    }

    public List<SoftwareProductSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try {
            return doql.query(query, resultSet -> {

                List<SoftwareProductSource> rows = new ArrayList<>(limit);

                while (resultSet.next()) {
                    rows.add(new SoftwareProductSource(
                            resultSet.getString("software_name"),
                            resultSet.getString("version"),
                            resultSet.getString("manufacturer")
                    ));
                }

                return rows;
            });
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "TLOAM 소프트웨어 카탈로그 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private static final String DEVICE_FILTER = """
            d.type IN ('virtual', 'physical')
            AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype NOT IN ('Network Printer', 'PDU')
            )
            """;

    private static final String CATALOG_SOURCE_QUERY = """
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

    private static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            FROM (
            """ + CATALOG_SOURCE_QUERY + """
            ) catalog
            """;

    private static final String SOURCE_QUERY = CATALOG_SOURCE_QUERY + """
            ORDER BY software_name, version, manufacturer
            """;
}
