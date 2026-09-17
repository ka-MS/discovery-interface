package com.itmsg.device42.integration.d42maximo.asset.logicaldrive;

import com.itmsg.device42.device42.DoqlClient;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class LogicalDriveQuery {

    private final DoqlClient doql;

    public LogicalDriveQuery(DoqlClient doql) {
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
            throw new IllegalStateException("DPA LogicalDrive 대상 마운트포인트 건수 조회에 실패했습니다.", e);
        }
    }

    public List<LogicalDriveSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try {
            return doql.query(query, resultSet -> {

                List<LogicalDriveSource> rows = new ArrayList<>(limit);

                while (resultSet.next()) {
                    rows.add(new LogicalDriveSource(
                            resultSet.getLong("mountpoint_pk"),
                            resultSet.getLong("device_fk"),
                            resultSet.getString("mountpoint"),
                            resultSet.getString("filesystem"),
                            resultSet.getString("fstype_name"),
                            resultSet.getBigDecimal("capacity"),
                            resultSet.getBigDecimal("free_capacity"),
                            resultSet.getString("label")
                    ));
                }

                return rows;
            });
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "DPA LogicalDrive 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
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
            FROM view_mountpoint_v2 m
            JOIN view_device_v2 d ON d.device_pk = ANY(m.device_fks)
            WHERE
            """ + DEVICE_FILTER + """
              AND LOWER(COALESCE(m.fstype_name, '')) NOT IN ('overlay', 'devtmpfs', 'efivarfs')
            """;

    private static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(DISTINCT m.mountpoint_pk)
            """ + SOURCE_FROM_AND_FILTER;

    private static final String SOURCE_QUERY = """
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
