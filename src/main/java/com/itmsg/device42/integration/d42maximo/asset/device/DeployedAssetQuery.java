package com.itmsg.device42.integration.d42maximo.asset.device;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import com.itmsg.device42.config.Device42ConnectionFactory;
import com.itmsg.device42.dto.device42.asset.DeviceSource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DeployedAssetQuery {

    public DeployedAssetQuery(Device42ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    private final Device42ConnectionFactory connectionFactory;

    public long getTotalCount() {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(DEVICE_TOTAL_COUNT_QUERY);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return resultSet.getLong(1);
            }

            return 0L;
        } catch (SQLException e) {
            throw new IllegalStateException("장비 건수 조회에 실패했습니다.", e);
        }
    }

    public List<DeviceSource> getData(long offset, int limit) {

        String query = DEVICE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            List<DeviceSource> devices = new ArrayList<>(limit);

            while (resultSet.next()) {
                devices.add(new DeviceSource(
                        resultSet.getInt("device_pk"),
                        resultSet.getString("name"),
                        resultSet.getString("type"),
                        resultSet.getString("notes"),
                        resultSet.getString("serial_no"),
                        resultSet.getString("asset_no"),
                        resultSet.getString("uuid"),
                        resultSet.getBoolean("network_device"),
                        resultSet.getString("physicalsubtype"),
                        resultSet.getString("hardware_name"),
                        resultSet.getString("vendor_name"),
                        resultSet.getBoolean("in_service"),
                        getNullableLocalDateTime(resultSet, "last_discovered")
                ));
            }

            return devices;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "데이터 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private static LocalDateTime getNullableLocalDateTime(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
            return value == null ? null : value.toLocalDateTime();
    }

    private static final String DEVICE_FILTER = """
            d.type IN ('virtual', 'physical')
            AND (
                d.virtualsubtype_id IS NULL
                OR d.virtualsubtype_id <> 15
            )
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype <> 'PDU'
            )
            """;

    private static final String DEVICE_TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            FROM view_device_v2 d
            WHERE
            """ + DEVICE_FILTER;

    private static final String DEVICE_QUERY = """
            SELECT
                d.device_pk,
                d.name,
                d.type,
                d.notes,
                d.serial_no,
                d.asset_no,
                d.uuid,
                d.network_device,
                d.physicalsubtype,
                h.name AS hardware_name,
                v.name AS vendor_name,
                d.in_service,
                d.last_discovered
            FROM view_device_v2 d
            LEFT JOIN view_hardware_v2 h
                ON d.hardware_fk = h.hardware_pk
            LEFT JOIN view_vendor_v1 v
                ON h.vendor_fk = v.vendor_pk
            WHERE
            """ + DEVICE_FILTER + """
            ORDER BY d.device_pk
            """;
}
