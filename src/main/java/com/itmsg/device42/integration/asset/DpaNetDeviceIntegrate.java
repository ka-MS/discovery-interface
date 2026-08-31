package com.itmsg.device42.integration.asset;

import com.itmsg.device42.dto.device42.Device42DpaNetDeviceSource;
import com.itmsg.device42.dto.maximo.DpaNetDeviceUpsert;
import com.itmsg.device42.config.Device42ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(3)
public class DpaNetDeviceIntegrate implements AssetIntegrationTask {

    private static final Logger log = LoggerFactory.getLogger(DpaNetDeviceIntegrate.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final Device42ConnectionFactory connectionFactory;
    private final JdbcTemplate maximoJdbcTemplate;

    public DpaNetDeviceIntegrate(
            Device42ConnectionFactory connectionFactory,
            @Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate
    ) {
        this.connectionFactory = connectionFactory;
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    @Override
    public void integrate() {
        long totalCount = getTotalCount();

        for (long offset = 0; offset < totalCount; offset += DEFAULT_BATCH_SIZE) {
            int limit = (int) Math.min(DEFAULT_BATCH_SIZE, totalCount - offset);
            List<Device42DpaNetDeviceSource> sourceData = getData(offset, limit);

            List<DpaNetDeviceUpsert> mappedData = mapData(sourceData);

            putData(mappedData);
        }
    }

    public long getTotalCount() {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(DEVICE_TOTAL_COUNT_QUERY);
             ResultSet resultSet = statement.executeQuery()) {

            return resultSet.next() ? resultSet.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new IllegalStateException("DPA NetDevice 대상 장비 건수 조회에 실패했습니다.", e);
        }
    }

    public List<Device42DpaNetDeviceSource> getData(long offset, int limit) {
        String query = DEVICE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            List<Device42DpaNetDeviceSource> devices = new ArrayList<>(limit);
            while (resultSet.next()) {
                devices.add(new Device42DpaNetDeviceSource(
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

    private List<DpaNetDeviceUpsert> mapData(List<Device42DpaNetDeviceSource> sourceData) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<DpaNetDeviceUpsert> mappedData = new ArrayList<>(sourceData.size());

        for (Device42DpaNetDeviceSource source : sourceData) {
            mappedData.add(new DpaNetDeviceUpsert(
                    source.devicePk().longValue(),
                    source.macAddress(),
                    source.networkAddress(),
                    source.osVersion(),
                    applyDateTime,
                    applyDateTime
            ));
        }

        return mappedData;
    }

    public void putData(List<DpaNetDeviceUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_DPA_NET_DEVICE_QUERY,
                (PreparedStatement statement) -> {
                    for (DpaNetDeviceUpsert device : data) {
                        try {
                            statement.setLong(1, device.nodeId());
                            statement.setString(2, device.netMacAddress());
                            statement.setString(3, device.networkAddress());
                            statement.setString(4, device.osVersion());
                            statement.setTimestamp(5, toTimestamp(device.createDate()));
                            statement.setTimestamp(6, toTimestamp(device.changeDate()));
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error(
                                    "DPA NetDevice MERGE에 실패했습니다. nodeId={}",
                                    device.nodeId(),
                                    e
                            );
                        }
                    }
                    return null;
                }
        );
    }

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
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

    private static final String MERGE_DPA_NET_DEVICE_QUERY = """
            MERGE INTO MAXIMO.DPANETDEVICE AS target
            USING (
                VALUES (?, ?, ?, ?, ?, ?)
            ) AS source (
                NODEID,
                NETMACADDR,
                NETWORKADDRESS,
                OSVERSION,
                CREATEDATE,
                CHANGEDATE
            )
            ON target.NODEID = source.NODEID
            WHEN MATCHED THEN
                UPDATE SET
                    NETMACADDR = source.NETMACADDR,
                    NETWORKADDRESS = source.NETWORKADDRESS,
                    OSVERSION = source.OSVERSION,
                    CHANGEDATE = source.CHANGEDATE
            WHEN NOT MATCHED THEN
                INSERT (
                    NODEID,
                    NETMACADDR,
                    NETWORKADDRESS,
                    OSVERSION,
                    CREATEDATE,
                    CHANGEDATE
                )
                VALUES (
                    source.NODEID,
                    source.NETMACADDR,
                    source.NETWORKADDRESS,
                    source.OSVERSION,
                    source.CREATEDATE,
                    source.CHANGEDATE
                )
            """;
}
