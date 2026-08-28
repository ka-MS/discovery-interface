package com.itmsg.device42.integration.software;

import com.itmsg.device42.dto.device42.Device42DpaSoftwareSource;
import com.itmsg.device42.dto.maximo.DpaSoftwareUpsert;
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
@Order(2)
public class DpaSoftwareIntegrate implements SoftwareIntegrationTask {

    private static final Logger log = LoggerFactory.getLogger(DpaSoftwareIntegrate.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final String UNKNOWN = "UNKNOWN";
    private static final long NO_SUITE = 0L;

    private final Device42ConnectionFactory connectionFactory;
    private final JdbcTemplate maximoJdbcTemplate;

    public DpaSoftwareIntegrate(
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
            log.info("배치할 DPA 소프트웨어 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        log.info("배치할 총 데이터. totalcount={}", totalCount);

        for (long offset = 0; offset < totalCount; offset += batchSize) {
            int limit = (int) Math.min(batchSize, totalCount - offset);

            List<Device42DpaSoftwareSource> data = getData(offset, limit);

            List<DpaSoftwareUpsert> mappedData = mapData(data);

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
            throw new IllegalStateException("DPA 소프트웨어 대상 건수 조회에 실패했습니다.", e);
        }
    }

    public List<Device42DpaSoftwareSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            List<Device42DpaSoftwareSource> rows = new ArrayList<>(limit);

            while (resultSet.next()) {
                rows.add(new Device42DpaSoftwareSource(
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
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "DPA 소프트웨어 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private List<DpaSoftwareUpsert> mapData(List<Device42DpaSoftwareSource> data) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<DpaSoftwareUpsert> mappedData = new ArrayList<>(data.size());

        for (Device42DpaSoftwareSource source : data) {
            mappedData.add(new DpaSoftwareUpsert(
                    source.softwareInUsePk(),
                    source.deviceFk(),
                    defaultUnknown(source.softwareName()),
                    defaultUnknown(source.vendorName()),
                    trimToNull(source.version()),
                    TloamSoftwareIntegrate.buildUniqueId(
                            source.softwareName(),
                            source.version(),
                            source.vendorName()
                    ),
                    trimToNull(source.installPath()),
                    source.installDate(),
                    source.firstDetected(),
                    source.lastUpdated(),
                    NO_SUITE,
                    applyDateTime,
                    applyDateTime
            ));
        }

        return mappedData;
    }

    public void putData(List<DpaSoftwareUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_DPA_SOFTWARE_QUERY,
                (PreparedStatement statement) -> {
                    for (DpaSoftwareUpsert software : data) {
                        try {
                            statement.setLong(1, software.softwareId());
                            statement.setTimestamp(2, toTimestamp(software.firstEncountered()));
                            statement.setTimestamp(3, toTimestamp(software.installDate()));
                            statement.setString(4, software.installPath());
                            statement.setTimestamp(5, toTimestamp(software.lastEncountered()));
                            statement.setString(6, software.manufacturer());
                            statement.setLong(7, software.nodeId());
                            statement.setString(8, software.softwareName());
                            statement.setLong(9, software.suiteId());
                            statement.setString(10, software.version());
                            statement.setTimestamp(11, toTimestamp(software.createDate()));
                            statement.setTimestamp(12, toTimestamp(software.changeDate()));
                            statement.setString(13, software.tloamUniqueId());
                            int mergedRows = statement.executeUpdate();
                            if (mergedRows == 0) {
                                log.error(
                                        "TLOAM 소프트웨어 카탈로그를 찾지 못해 DPA 소프트웨어를 MERGE하지 않았습니다. "
                                                + "softwareId={}, nodeId={}, uniqueId={}",
                                        software.softwareId(),
                                        software.nodeId(),
                                        software.tloamUniqueId()
                                );
                            }
                        } catch (SQLException e) {
                            log.error(
                                    "DPA 소프트웨어 MERGE에 실패했습니다. softwareId={}, nodeId={}",
                                    software.softwareId(),
                                    software.nodeId(),
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

    private static LocalDateTime getNullableLocalDateTime(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
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

    private static final String MERGE_DPA_SOFTWARE_QUERY = """
            MERGE INTO MAXIMO.DPASOFTWARE AS target
            USING (
                SELECT
                    input.SOFTWAREID,
                    input.FIRSTENCOUNTERED1,
                    input.INSTALLDATE,
                    input.INSTALLPATH,
                    input.LASTENCOUNTERED1,
                    input.MANUFACTURER,
                    input.NODEID,
                    input.SOFTWARENAME,
                    input.SUITEID,
                    input.VERSION,
                    input.CREATEDATE,
                    input.CHANGEDATE,
                    catalog.TLOAMSOFTWAREID,
                    catalog.TLOAMSOFTWAREID AS TLOAMPRODUCTID
                FROM (
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ) AS input (
                    SOFTWAREID,
                    FIRSTENCOUNTERED1,
                    INSTALLDATE,
                    INSTALLPATH,
                    LASTENCOUNTERED1,
                    MANUFACTURER,
                    NODEID,
                    SOFTWARENAME,
                    SUITEID,
                    VERSION,
                    CREATEDATE,
                    CHANGEDATE,
                    TLOAMUNIQUEID
                )
                JOIN MAXIMO.TLOAMSOFTWARE catalog
                  ON catalog.UNIQUEID = input.TLOAMUNIQUEID
            ) AS source
            ON target.SOFTWAREID = source.SOFTWAREID
            WHEN MATCHED THEN
                UPDATE SET
                    FIRSTENCOUNTERED1 = source.FIRSTENCOUNTERED1,
                    INSTALLDATE = source.INSTALLDATE,
                    INSTALLPATH = source.INSTALLPATH,
                    LASTENCOUNTERED1 = source.LASTENCOUNTERED1,
                    MANUFACTURER = source.MANUFACTURER,
                    NODEID = source.NODEID,
                    SOFTWARENAME = source.SOFTWARENAME,
                    SUITEID = source.SUITEID,
                    VERSION = source.VERSION,
                    TLOAMSOFTWAREID = source.TLOAMSOFTWAREID,
                    TLOAMPRODUCTID = source.TLOAMPRODUCTID,
                    CHANGEDATE = source.CHANGEDATE
            WHEN NOT MATCHED THEN
                INSERT (
                    SOFTWAREID,
                    FIRSTENCOUNTERED1,
                    INSTALLDATE,
                    INSTALLPATH,
                    LASTENCOUNTERED1,
                    MANUFACTURER,
                    NODEID,
                    SOFTWARENAME,
                    SUITEID,
                    VERSION,
                    TLOAMSOFTWAREID,
                    TLOAMPRODUCTID,
                    CREATEDATE,
                    CHANGEDATE
                )
                VALUES (
                    source.SOFTWAREID,
                    source.FIRSTENCOUNTERED1,
                    source.INSTALLDATE,
                    source.INSTALLPATH,
                    source.LASTENCOUNTERED1,
                    source.MANUFACTURER,
                    source.NODEID,
                    source.SOFTWARENAME,
                    source.SUITEID,
                    source.VERSION,
                    source.TLOAMSOFTWAREID,
                    source.TLOAMPRODUCTID,
                    source.CREATEDATE,
                    source.CHANGEDATE
                )
            """;
}
