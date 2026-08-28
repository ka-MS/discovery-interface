package com.itmsg.device42.integration.asset;

import com.itmsg.device42.dto.device42.Device42DpaOsSource;
import com.itmsg.device42.dto.maximo.DpaOsUpsert;
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
@Order(11)
public class DpaOsIntegrate implements AssetIntegrationTask {

    private static final Logger log = LoggerFactory.getLogger(DpaOsIntegrate.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final String UNKNOWN = "UNKNOWN";

    private final Device42ConnectionFactory connectionFactory;
    private final JdbcTemplate maximoJdbcTemplate;

    public DpaOsIntegrate(
            Device42ConnectionFactory connectionFactory,
            @Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate
    ) {
        this.connectionFactory = connectionFactory;
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    @Override
    public void integrate() {
        long totalCount = getTotalCount();
        int batchSize = DEFAULT_BATCH_SIZE;

        if (totalCount <= 0) {
            log.info("배치할 DPA OS 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        for (long offset = 0; offset < totalCount; offset += batchSize) {
            int limit = (int) Math.min(batchSize, totalCount - offset);

            List<Device42DpaOsSource> data = getData(offset, limit);

            List<DpaOsUpsert> mappedData = mapData(data);

            putData(mappedData);
        }
    }

    public long getTotalCount() {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(TOTAL_COUNT_QUERY);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return resultSet.getLong(1);
            }

            return 0L;
        } catch (SQLException e) {
            throw new IllegalStateException("DPA OS 대상 건수 조회에 실패했습니다.", e);
        }
    }

    public List<Device42DpaOsSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            List<Device42DpaOsSource> rows = new ArrayList<>(limit);

            while (resultSet.next()) {
                rows.add(new Device42DpaOsSource(
                        resultSet.getLong("deviceos_pk"),
                        resultSet.getLong("device_fk"),
                        resultSet.getString("os_name"),
                        resultSet.getString("os_version"),
                        resultSet.getString("os_version_no"),
                        resultSet.getString("vendor_name")
                ));
            }

            return rows;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "DPA OS 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private List<DpaOsUpsert> mapData(List<Device42DpaOsSource> data) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<DpaOsUpsert> mappedData = new ArrayList<>(data.size());

        for (Device42DpaOsSource source : data) {
            mappedData.add(new DpaOsUpsert(
                    source.deviceosPk(),
                    source.deviceFk(),
                    trimToNull(source.osVersionNo()),
                    defaultUnknown(source.vendorName()),
                    defaultUnknown(source.osName()),
                    trimToNull(source.osVersion()),
                    applyDateTime,
                    applyDateTime
            ));
        }

        return mappedData;
    }

    public void putData(List<DpaOsUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_DPA_OS_QUERY,
                (PreparedStatement statement) -> {
                    for (DpaOsUpsert os : data) {
                        try {
                            statement.setLong(1, os.osId());
                            statement.setString(2, os.build());
                            statement.setString(3, os.manufacturer());
                            statement.setString(4, os.name());
                            statement.setLong(5, os.nodeId());
                            statement.setString(6, os.version());
                            statement.setTimestamp(7, toTimestamp(os.createDate()));
                            statement.setTimestamp(8, toTimestamp(os.changeDate()));
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error(
                                    "DPA OS MERGE에 실패했습니다. osId={}, nodeId={}",
                                    os.osId(),
                                    os.nodeId(),
                                    e
                            );
                        }
                    }
                    return null;
                }
        );
    }

    static String defaultUnknown(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? UNKNOWN : normalized;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
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

    private static final String MERGE_DPA_OS_QUERY = """
            MERGE INTO MAXIMO.DPAOS AS target
            USING (
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ) AS source (
                OSID,
                BUILD,
                MANUFACTURER,
                NAME,
                NODEID,
                VERSION,
                CREATEDATE,
                CHANGEDATE
            )
            ON target.OSID = source.OSID
            WHEN MATCHED THEN
                UPDATE SET
                    BUILD = source.BUILD,
                    MANUFACTURER = source.MANUFACTURER,
                    NAME = source.NAME,
                    NODEID = source.NODEID,
                    VERSION = source.VERSION,
                    CHANGEDATE = source.CHANGEDATE
            WHEN NOT MATCHED THEN
                INSERT (
                    OSID,
                    BUILD,
                    MANUFACTURER,
                    NAME,
                    NODEID,
                    VERSION,
                    CREATEDATE,
                    CHANGEDATE
                )
                VALUES (
                    source.OSID,
                    source.BUILD,
                    source.MANUFACTURER,
                    source.NAME,
                    source.NODEID,
                    source.VERSION,
                    source.CREATEDATE,
                    source.CHANGEDATE
                )
            """;
}
