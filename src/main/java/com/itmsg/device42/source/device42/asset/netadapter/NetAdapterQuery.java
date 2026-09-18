package com.itmsg.device42.source.device42.asset.netadapter;

import com.itmsg.device42.source.device42.selection.DeviceSelection;

import com.itmsg.device42.source.device42.DoqlClient;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NetAdapterQuery {

    private final DoqlClient doql;
    private final DeviceSelection selection;

    public NetAdapterQuery(DeviceSelection selection, DoqlClient doql) {
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
            throw new IllegalStateException("DPA 네트워크 어댑터 대상 포트 건수 조회에 실패했습니다.", e);
        }
    }

    public List<NetworkInterfaceSource> getData(long offset, int limit) {
        String query = sql(SOURCE_QUERY) + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try {
            return doql.query(query, resultSet -> {

                List<NetworkInterfaceSource> rows = new ArrayList<>(limit);

                while (resultSet.next()) {
                    rows.add(new NetworkInterfaceSource(
                            resultSet.getLong("netport_pk"),
                            resultSet.getLong("device_fk"),
                            resultSet.getString("port"),
                            resultSet.getString("description"),
                            resultSet.getString("hwaddress"),
                            resultSet.getString("hwaddress2"),
                            resultSet.getString("port_speed"),
                            resultSet.getString("global_type"),
                            resultSet.getString("vendor_name")
                    ));
                }

                return rows;
            });
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "DPA 네트워크 어댑터 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private String sql(String template) {
        return template
                .replace("{{DEVICE_FILTER}}", selection.sql())
                .replace("{{COMPUTER_FILTER}}", selection.sql());
    }

    private static final String DEVICE_FILTER = "{{DEVICE_FILTER}}";

    private static final String SOURCE_FROM_AND_FILTER = """
            FROM view_netport_v1 n
            JOIN view_device_v2 d ON d.device_pk = n.device_fk
            LEFT JOIN view_vendor_v1 v ON v.vendor_pk = n.vendor_fk
            WHERE
            """ + DEVICE_FILTER;

    private static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            """ + SOURCE_FROM_AND_FILTER;

    private static final String SOURCE_QUERY = """
            SELECT
                n.netport_pk,
                n.device_fk,
                n.port,
                n.description,
                n.hwaddress,
                n.hwaddress2,
                n.port_speed,
                n.global_type,
                v.name AS vendor_name
            """ + SOURCE_FROM_AND_FILTER + """
            ORDER BY n.device_fk, n.netport_pk
            """;
}
