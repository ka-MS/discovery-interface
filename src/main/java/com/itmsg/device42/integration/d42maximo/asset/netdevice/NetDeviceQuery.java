package com.itmsg.device42.integration.d42maximo.asset.netdevice;

import com.itmsg.device42.config.Device42ConnectionFactory;
import com.itmsg.device42.dto.device42.asset.NetworkDeviceSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NetDeviceQuery {

    public NetDeviceQuery(Device42ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    private final Device42ConnectionFactory connectionFactory;

    public long getTotalCount() {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(DEVICE_TOTAL_COUNT_QUERY);
             ResultSet resultSet = statement.executeQuery()) {

            return resultSet.next() ? resultSet.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new IllegalStateException("DPA NetDevice 대상 장비 건수 조회에 실패했습니다.", e);
        }
    }

    public List<NetworkDeviceSource> getData(long offset, int limit) {
        String query = DEVICE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            List<NetworkDeviceSource> devices = new ArrayList<>(limit);
            while (resultSet.next()) {
                devices.add(new NetworkDeviceSource(
                        resultSet.getInt("device_pk"),
                        resultSet.getString("os_version"),
                        resultSet.getString("mac"),
                        resultSet.getString("mgmt_ip")
                ));
            }
            return devices;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "DPA NetDevice 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private static final String DEVICE_TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            FROM view_device_v2 d
            WHERE d.network_device = true
              AND d.type = 'physical'
            """;

    private static final String DEVICE_QUERY = """
            WITH target AS (
                SELECT device_pk, os_version
                FROM view_device_v2
                WHERE network_device = true
                  AND type = 'physical'
            ),
            link AS (
                SELECT second_device_fk AS physical_pk,
                       device_fk AS cluster_pk,
                       MIN(hwaddress) AS mac
                FROM view_netport_v1
                WHERE second_device_fk IS NOT NULL
                  AND hwaddress IS NOT NULL
                  AND hwaddress <> ''
                GROUP BY second_device_fk, device_fk
            )
            SELECT t.device_pk,
                   t.os_version,
                   l.mac,
                   (SELECT MIN(ip_address)
                      FROM view_ipaddress_v2
                     WHERE l.cluster_pk = ANY(device_fks)) AS mgmt_ip
            FROM target t
            LEFT JOIN link l ON l.physical_pk = t.device_pk
            ORDER BY t.device_pk
            """;
}
