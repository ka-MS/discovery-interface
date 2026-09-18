package com.itmsg.device42.source.device42.software.installed;

import com.itmsg.device42.source.device42.selection.DeviceSelection;

import com.itmsg.device42.source.device42.DoqlClient;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DpaSoftwareQuery {

    private final DoqlClient doql;
    private final DeviceSelection selection;

    public DpaSoftwareQuery(DeviceSelection selection, DoqlClient doql) {
        this.doql = doql;
        this.selection = selection;
    }

    public long getTotalCount() {
        try {
            return doql.preparedQuery(sql(TOTAL_COUNT_QUERY), resultSet -> {

                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }

                return 0L;
            });
        } catch (SQLException e) {
            throw new IllegalStateException("DPA 소프트웨어 대상 건수 조회에 실패했습니다.", e);
        }
    }

    public List<InstalledSoftwareSource> getData(long offset, int limit) {
        String query = sql(SOURCE_QUERY) + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try {
            return doql.query(query, resultSet -> {

                List<InstalledSoftwareSource> rows = new ArrayList<>(limit);

                while (resultSet.next()) {
                    rows.add(new InstalledSoftwareSource(
                            resultSet.getLong("softwareinuse_pk"),
                            resultSet.getLong("device_fk"),
                            resultSet.getString("software_name"),
                            resultSet.getString("version"),
                            resultSet.getString("install_path"),
                            getNullableLocalDateTime(resultSet, "install_date"),
                            getNullableLocalDateTime(resultSet, "first_detected"),
                            getNullableLocalDateTime(resultSet, "last_updated"),
                            resultSet.getString("vendor_name")
                    ));
                }

                return rows;
            });
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "DPA 소프트웨어 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private static LocalDateTime getNullableLocalDateTime(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private String sql(String template) {
        return template
                .replace("{{DEVICE_FILTER}}", selection.sql())
                .replace("{{COMPUTER_FILTER}}", selection.sql());
    }

    private static final String DEVICE_FILTER = "{{DEVICE_FILTER}}";

    private static final String SOURCE_FROM_AND_FILTER = """
            FROM view_softwareinuse_v1 u
            JOIN view_device_v2 d ON d.device_pk = u.device_fk
            LEFT JOIN view_software_v1 s ON s.software_pk = u.software_fk
            LEFT JOIN view_vendor_v1 v ON v.vendor_pk = s.vendor_fk
            WHERE
            """ + DEVICE_FILTER;

    private static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            """ + SOURCE_FROM_AND_FILTER;

    private static final String SOURCE_QUERY = """
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
