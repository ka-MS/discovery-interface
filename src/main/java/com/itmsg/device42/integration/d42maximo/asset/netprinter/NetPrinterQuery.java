package com.itmsg.device42.integration.d42maximo.asset.netprinter;

import com.itmsg.device42.config.Device42ConnectionFactory;
import com.itmsg.device42.dto.device42.asset.NetworkPrinterSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NetPrinterQuery {

    public NetPrinterQuery(Device42ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    private final Device42ConnectionFactory connectionFactory;

    public long getTotalCount() {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(DEVICE_TOTAL_COUNT_QUERY);
             ResultSet resultSet = statement.executeQuery()) {

            return resultSet.next() ? resultSet.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new IllegalStateException("DPA NetPrinter 대상 장비 건수 조회에 실패했습니다.", e);
        }
    }

    public List<NetworkPrinterSource> getData(long offset, int limit) {
        String query = DEVICE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

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
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "DPA NetPrinter 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private static final String DEVICE_TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            FROM view_device_v2 d
            WHERE d.type IN ('virtual', 'physical')
              AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
              AND (d.network_device = false OR d.network_device IS NULL)
              AND d.physicalsubtype = 'Network Printer'
            """;

    private static final String DEVICE_QUERY = """
            SELECT
                d.device_pk,
                d.ram,
                d.ram_size_type,
                (SELECT UPPER(n.hwaddress)
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
            WHERE d.type IN ('virtual', 'physical')
              AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
              AND (d.network_device = false OR d.network_device IS NULL)
              AND d.physicalsubtype = 'Network Printer'
            ORDER BY d.device_pk
            """;
}
