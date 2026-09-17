package com.itmsg.device42.integration.d42maximo.asset.os;

import com.itmsg.device42.device42.DoqlClient;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OsQuery {

    private final DoqlClient doql;

    public OsQuery(DoqlClient doql) {
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
            throw new IllegalStateException("DPA OS 대상 건수 조회에 실패했습니다.", e);
        }
    }

    public List<OperatingSystemSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try {
            return doql.query(query, resultSet -> {

                List<OperatingSystemSource> rows = new ArrayList<>(limit);

                while (resultSet.next()) {
                    rows.add(new OperatingSystemSource(
                            resultSet.getLong("deviceos_pk"),
                            resultSet.getLong("device_fk"),
                            resultSet.getString("os_name"),
                            resultSet.getString("os_version"),
                            resultSet.getString("os_version_no"),
                            resultSet.getString("vendor_name")
                    ));
                }

                return rows;
            });
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "DPA OS 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
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

    private static final String SOURCE_FROM_AND_FILTER = """
            FROM view_deviceos_v1 o
            JOIN view_device_v2 d ON d.device_pk = o.device_fk
            LEFT JOIN view_os_v1 s ON s.os_pk = o.os_fk
            LEFT JOIN view_vendor_v1 v ON v.vendor_pk = s.vendor_fk
            WHERE
            """ + DEVICE_FILTER;

    private static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            """ + SOURCE_FROM_AND_FILTER;

    private static final String SOURCE_QUERY = """
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
