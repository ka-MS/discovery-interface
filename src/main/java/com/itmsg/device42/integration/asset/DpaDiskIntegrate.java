package com.itmsg.device42.integration.asset;

import com.itmsg.device42.dto.device42.Device42DpaDiskSource;
import com.itmsg.device42.dto.maximo.DpaDiskUpsert;
import com.itmsg.device42.config.Device42ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
@Order(6)
public class DpaDiskIntegrate implements AssetIntegrationTask {

    private static final Logger log = LoggerFactory.getLogger(DpaDiskIntegrate.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final int FALSE = 0;
    private static final String UNKNOWN = "UNKNOWN";

    private final Device42ConnectionFactory connectionFactory;
    private final JdbcTemplate maximoJdbcTemplate;

    public DpaDiskIntegrate(
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
            log.info("배치할 DPA Disk 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        for (long offset = 0; offset < totalCount; offset += batchSize) {
            int limit = (int) Math.min(batchSize, totalCount - offset);

            List<Device42DpaDiskSource> data = getData(offset, limit);

            List<DpaDiskUpsert> mappedData = mapData(data);

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
            throw new IllegalStateException("DPA Disk 대상 파트 건수 조회에 실패했습니다.", e);
        }
    }

    public List<Device42DpaDiskSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            List<Device42DpaDiskSource> rows = new ArrayList<>(limit);

            while (resultSet.next()) {
                rows.add(new Device42DpaDiskSource(
                        resultSet.getLong("part_pk"),
                        resultSet.getLong("device_fk"),
                        resultSet.getString("serial_no"),
                        resultSet.getString("description"),
                        resultSet.getString("model_name"),
                        resultSet.getBigDecimal("hdsize"),
                        resultSet.getString("hdsize_unit"),
                        resultSet.getString("hddtype_name"),
                        resultSet.getString("media_type_name"),
                        resultSet.getString("vendor_name")
                ));
            }

            return rows;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "DPA Disk 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private List<DpaDiskUpsert> mapData(List<Device42DpaDiskSource> data) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<DpaDiskUpsert> mappedData = new ArrayList<>(data.size());

        for (Device42DpaDiskSource source : data) {
            mappedData.add(new DpaDiskUpsert(
                    source.partPk(),
                    source.deviceFk(),
                    firstNonBlank(source.description(), source.modelName()),
                    trimToNull(source.diskInterface()),
                    FALSE,
                    FALSE,
                    trimToNull(source.modelName()),
                    defaultUnknown(source.vendorName()),
                    FALSE,
                    trimToNull(source.serialNumber()),
                    trimToNull(source.sizeUnit()),
                    roundTotalSpace(source.totalSpace()),
                    FALSE,
                    applyDateTime,
                    applyDateTime
            ));
        }

        return mappedData;
    }

    public void putData(List<DpaDiskUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_DPA_DISK_QUERY,
                (PreparedStatement statement) -> {
                    for (DpaDiskUpsert disk : data) {
                        try {
                            statement.setLong(1, disk.diskId());
                            statement.setString(2, disk.description());
                            statement.setString(3, disk.diskInterface());
                            statement.setObject(4, disk.externalDevice(), java.sql.Types.INTEGER);
                            statement.setObject(5, disk.hotSwappable(), java.sql.Types.INTEGER);
                            statement.setString(6, disk.makeModel());
                            statement.setString(7, disk.manufacturer());
                            statement.setLong(8, disk.nodeId());
                            statement.setObject(9, disk.removableMedia(), java.sql.Types.INTEGER);
                            statement.setString(10, disk.serialNumber());
                            statement.setString(11, disk.sizeUnit());
                            statement.setBigDecimal(12, disk.totalSpace());
                            statement.setObject(13, disk.writeCapable(), java.sql.Types.INTEGER);
                            statement.setTimestamp(14, toTimestamp(disk.createDate()));
                            statement.setTimestamp(15, toTimestamp(disk.changeDate()));
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error(
                                    "DPA Disk MERGE에 실패했습니다. diskId={}, nodeId={}",
                                    disk.diskId(),
                                    disk.nodeId(),
                                    e
                            );
                        }
                    }
                    return null;
                }
        );
    }

    static BigDecimal roundTotalSpace(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    static String firstNonBlank(String primary, String fallback) {
        String normalizedPrimary = trimToNull(primary);
        return normalizedPrimary == null ? trimToNull(fallback) : normalizedPrimary;
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
            FROM view_part_v1 p
            JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
            JOIN view_device_v2 d ON d.device_pk = p.device_fk
            LEFT JOIN view_vendor_v1 v ON v.vendor_pk = pm.vendor_fk
            WHERE pm.type_name = 'Hard Disk'
              AND
            """ + DEVICE_FILTER;

    private static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            """ + SOURCE_FROM_AND_FILTER;

    private static final String SOURCE_QUERY = """
            SELECT
                p.part_pk,
                p.device_fk,
                p.serial_no,
                p.description,
                pm.name AS model_name,
                pm.hdsize,
                pm.hdsize_unit,
                pm.hddtype_name,
                pm.media_type_name,
                v.name AS vendor_name
            """ + SOURCE_FROM_AND_FILTER + """
            ORDER BY p.device_fk, pm.name, p.part_pk
            """;

    private static final String MERGE_DPA_DISK_QUERY = """
            MERGE INTO MAXIMO.DPADISK AS target
            USING (
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ) AS source (
                DISKID,
                DESCRIPTION,
                DISKINTERFACE,
                EXTERNALDEVICE,
                HOTSWAPPABLE,
                MAKEMODEL,
                MANUFACTURER,
                NODEID,
                REMOVABLEMEDIA,
                SERIALNUMBER,
                SIZEUNIT,
                TOTALSPACE,
                WRITECAPABLE,
                CREATEDATE,
                CHANGEDATE
            )
            ON target.DISKID = source.DISKID
            WHEN MATCHED THEN
                UPDATE SET
                    DESCRIPTION = source.DESCRIPTION,
                    DISKINTERFACE = source.DISKINTERFACE,
                    EXTERNALDEVICE = source.EXTERNALDEVICE,
                    HOTSWAPPABLE = source.HOTSWAPPABLE,
                    MAKEMODEL = source.MAKEMODEL,
                    MANUFACTURER = source.MANUFACTURER,
                    NODEID = source.NODEID,
                    REMOVABLEMEDIA = source.REMOVABLEMEDIA,
                    SERIALNUMBER = source.SERIALNUMBER,
                    SIZEUNIT = source.SIZEUNIT,
                    TOTALSPACE = source.TOTALSPACE,
                    WRITECAPABLE = source.WRITECAPABLE,
                    CHANGEDATE = source.CHANGEDATE
            WHEN NOT MATCHED THEN
                INSERT (
                    DISKID,
                    DESCRIPTION,
                    DISKINTERFACE,
                    EXTERNALDEVICE,
                    HOTSWAPPABLE,
                    MAKEMODEL,
                    MANUFACTURER,
                    NODEID,
                    REMOVABLEMEDIA,
                    SERIALNUMBER,
                    SIZEUNIT,
                    TOTALSPACE,
                    WRITECAPABLE,
                    CREATEDATE,
                    CHANGEDATE
                )
                VALUES (
                    source.DISKID,
                    source.DESCRIPTION,
                    source.DISKINTERFACE,
                    source.EXTERNALDEVICE,
                    source.HOTSWAPPABLE,
                    source.MAKEMODEL,
                    source.MANUFACTURER,
                    source.NODEID,
                    source.REMOVABLEMEDIA,
                    source.SERIALNUMBER,
                    source.SIZEUNIT,
                    source.TOTALSPACE,
                    source.WRITECAPABLE,
                    source.CREATEDATE,
                    source.CHANGEDATE
                )
            """;
}
