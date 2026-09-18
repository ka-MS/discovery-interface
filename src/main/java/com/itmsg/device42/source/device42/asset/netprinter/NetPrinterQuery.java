package com.itmsg.device42.source.device42.asset.netprinter;

import com.itmsg.device42.source.device42.selection.DeviceSelection;

import com.itmsg.device42.source.device42.DoqlClient;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NetPrinterQuery {

    private final DoqlClient doql;
    private final DeviceSelection selection;

    public NetPrinterQuery(DeviceSelection selection, DoqlClient doql) {
        this.doql = doql;
        this.selection = selection;
    }

    public long getTotalCount() {
        try {
            return doql.preparedQuery(sql(DEVICE_TOTAL_COUNT_QUERY), resultSet -> {

                return resultSet.next() ? resultSet.getLong(1) : 0L;
            });
        } catch (SQLException e) {
            throw new IllegalStateException("DPA NetPrinter 대상 장비 건수 조회에 실패했습니다.", e);
        }
    }

    public List<NetworkPrinterSource> getData(long offset, int limit) {
        String query = sql(DEVICE_QUERY) + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try {
            return doql.query(query, resultSet -> {

                List<NetworkPrinterSource> printers = new ArrayList<>(limit);
                while (resultSet.next()) {
                    printers.add(new NetworkPrinterSource(
                            resultSet.getInt("device_pk"),
                            resultSet.getBigDecimal("ram"),
                            resultSet.getString("ram_size_type"),
                            resultSet.getString("hwaddress"),
                            resultSet.getString("ip_address"),
                            resultSet.getInt("tray_cnt")
                    ));
                }
                return printers;
            });
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "DPA NetPrinter 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private String sql(String template) {
        return template
                .replace("{{DEVICE_FILTER}}", selection.sql())
                .replace("{{COMPUTER_FILTER}}", selection.sql());
    }

    private static final String DEVICE_TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            FROM view_device_v2 d
            WHERE {{DEVICE_FILTER}}
            """;

    private static final String DEVICE_QUERY = """
            SELECT
                d.device_pk,
                d.ram,
                d.ram_size_type,
                (SELECT n.hwaddress
                 FROM view_netport_v1 n
                 WHERE n.device_fk = d.device_pk AND n.hwaddress <> ''
                 LIMIT 1) AS hwaddress,
                (SELECT HOST(i.ip_address)
                 FROM view_ipaddress_v2 i
                 WHERE d.device_pk = ANY(i.device_fks)
                 LIMIT 1) AS ip_address,
                (SELECT COUNT(*)
                 FROM view_part_v1 p
                 JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
                 WHERE p.device_fk = d.device_pk AND pm.type_name = 'printer_input') AS tray_cnt
            FROM view_device_v2 d
            WHERE {{DEVICE_FILTER}}
            ORDER BY d.device_pk
            """;
}
