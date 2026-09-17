package com.itmsg.device42.integration.d42maximo.asset.device;

import com.itmsg.device42.device42.DoqlClient;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DeployedAssetQuery {

    private final DoqlClient doql;

    public DeployedAssetQuery(DoqlClient doql) {
        this.doql = doql;
    }

    public long getTotalCount() {
        try {
            return doql.preparedQuery(DEVICE_TOTAL_COUNT_QUERY, resultSet -> {

                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }

                return 0L;
            });
        } catch (SQLException e) {
            throw new IllegalStateException("장비 건수 조회에 실패했습니다.", e);
        }
    }

    public List<DeviceSource> getData(long offset, int limit) {

        String query = DEVICE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try {
            return doql.query(query, resultSet -> {

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
            });
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
