package com.itmsg.device42.integration.d42maximo.conversion.os;

import com.itmsg.device42.device42.DoqlClient;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DpamOsQuery {

    private final DoqlClient doql;

    public DpamOsQuery(DoqlClient doql) {
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
            throw new IllegalStateException("운영체제 변환 대상 건수 조회에 실패했습니다.", e);
        }
    }

    public List<OperatingSystemNameSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try {
            return doql.query(query, resultSet -> {

                List<OperatingSystemNameSource> rows = new ArrayList<>(limit);

                while (resultSet.next()) {
                    rows.add(new OperatingSystemNameSource(resultSet.getString("name")));
                }

                return rows;
            });
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "운영체제 변환 대상 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private static final String SOURCE_QUERY = """
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

    private static final String TOTAL_COUNT_QUERY = """
            WITH source AS (
""" + SOURCE_QUERY + """
            )
            SELECT COUNT(*) FROM source
            """;
}
