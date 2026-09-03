package com.itmsg.device42.integration.asset;

import com.itmsg.device42.dto.device42.asset.LogicalDriveSource;
import com.itmsg.device42.dto.maximo.DpaLogicalDriveUpsert;
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
import java.util.Locale;

@Component
@Order(10)
public class DpaLogicalDriveIntegrate implements AssetIntegrationTask {

    private static final Logger log = LoggerFactory.getLogger(DpaLogicalDriveIntegrate.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final int FALSE = 0;
    private static final int VOLUME_LABEL_MAX_LENGTH = 16;
    private static final String DRIVE_TYPE = "UNKNOWN";
    private static final String SIZE_UNIT = "MB";

    private final Device42ConnectionFactory connectionFactory;
    private final JdbcTemplate maximoJdbcTemplate;

    public DpaLogicalDriveIntegrate(
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
            log.info("배치할 DPA LogicalDrive 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        for (long offset = 0; offset < totalCount; offset += batchSize) {
            int limit = (int) Math.min(batchSize, totalCount - offset);

            List<LogicalDriveSource> data = getData(offset, limit);

            List<DpaLogicalDriveUpsert> mappedData = mapData(data);

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
            throw new IllegalStateException("DPA LogicalDrive 대상 마운트포인트 건수 조회에 실패했습니다.", e);
        }
    }

    public List<LogicalDriveSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            List<LogicalDriveSource> rows = new ArrayList<>(limit);

            while (resultSet.next()) {
                rows.add(new LogicalDriveSource(
                        resultSet.getLong("mountpoint_pk"),
                        resultSet.getLong("device_fk"),
                        resultSet.getString("mountpoint"),
                        resultSet.getString("filesystem"),
                        resultSet.getString("fstype_name"),
                        resultSet.getBigDecimal("capacity"),
                        resultSet.getBigDecimal("free_capacity"),
                        resultSet.getString("label")
                ));
            }

            return rows;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "DPA LogicalDrive 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private List<DpaLogicalDriveUpsert> mapData(List<LogicalDriveSource> data) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<DpaLogicalDriveUpsert> mappedData = new ArrayList<>(data.size());

        for (LogicalDriveSource source : data) {
            mappedData.add(new DpaLogicalDriveUpsert(
                    source.mountPointPk(),
                    source.deviceFk(),
                    attachedNetworkName(source.fileSystem(), source.fileSystemType()),
                    roundSize(source.availableSize()),
                    FALSE,
                    DRIVE_TYPE,
                    FALSE,
                    trimToNull(source.fileSystemType()),
                    trimToNull(source.mountPoint()),
                    SIZE_UNIT,
                    roundSize(source.totalSize()),
                    truncate(source.volumeLabel(), VOLUME_LABEL_MAX_LENGTH),
                    applyDateTime,
                    applyDateTime
            ));
        }

        return mappedData;
    }

    public void putData(List<DpaLogicalDriveUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_DPA_LOGICAL_DRIVE_QUERY,
                (PreparedStatement statement) -> {
                    for (DpaLogicalDriveUpsert drive : data) {
                        try {
                            statement.setLong(1, drive.logicalDriveId());
                            statement.setString(2, drive.attachedNetworkName());
                            statement.setBigDecimal(3, drive.availableSize());
                            statement.setObject(4, drive.compressed(), java.sql.Types.INTEGER);
                            statement.setString(5, drive.driveType());
                            statement.setObject(6, drive.encrypted(), java.sql.Types.INTEGER);
                            statement.setString(7, drive.fileSystem());
                            statement.setString(8, drive.mount());
                            statement.setLong(9, drive.nodeId());
                            statement.setString(10, drive.sizeUnit());
                            statement.setBigDecimal(11, drive.totalSize());
                            statement.setString(12, drive.volumeLabel());
                            statement.setTimestamp(13, toTimestamp(drive.createDate()));
                            statement.setTimestamp(14, toTimestamp(drive.changeDate()));
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error(
                                    "DPA LogicalDrive MERGE에 실패했습니다. logicalDriveId={}, nodeId={}",
                                    drive.logicalDriveId(),
                                    drive.nodeId(),
                                    e
                            );
                        }
                    }
                    return null;
                }
        );
    }

    static BigDecimal roundSize(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    static String attachedNetworkName(String fileSystem, String fileSystemType) {
        String normalizedType = trimToNull(fileSystemType);
        if (normalizedType == null) {
            return null;
        }

        String lowerType = normalizedType.toLowerCase(Locale.ROOT);
        if (!lowerType.equals("nfs") && !lowerType.equals("nfs4")) {
            return null;
        }

        return trimToNull(fileSystem);
    }

    static String truncate(String value, int maxLength) {
        String normalized = trimToNull(value);
        if (normalized == null || normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
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
            FROM view_mountpoint_v2 m
            JOIN view_device_v2 d ON d.device_pk = ANY(m.device_fks)
            WHERE
            """ + DEVICE_FILTER + """
              AND LOWER(COALESCE(m.fstype_name, '')) NOT IN ('overlay', 'devtmpfs', 'efivarfs')
            """;

    private static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(DISTINCT m.mountpoint_pk)
            """ + SOURCE_FROM_AND_FILTER;

    private static final String SOURCE_QUERY = """
            SELECT DISTINCT ON (m.mountpoint_pk)
                m.mountpoint_pk,
                d.device_pk AS device_fk,
                m.mountpoint,
                m.filesystem,
                m.fstype_name,
                m.capacity,
                m.free_capacity,
                m.label
            """ + SOURCE_FROM_AND_FILTER + """
            ORDER BY m.mountpoint_pk, d.device_pk
            """;

    private static final String MERGE_DPA_LOGICAL_DRIVE_QUERY = """
            MERGE INTO MAXIMO.DPALOGICALDRIVE AS target
            USING (
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ) AS source (
                LOGICALDRIVEID,
                ATTACHEDNETNAME,
                AVAILABLESIZE,
                COMPRESSED,
                DRIVETYPE,
                ENCRYPTED,
                FILESYSTEM,
                MOUNT,
                NODEID,
                SIZEUNIT,
                TOTALSIZE,
                VOLUMELABEL,
                CREATEDATE,
                CHANGEDATE
            )
            ON target.LOGICALDRIVEID = source.LOGICALDRIVEID
            WHEN MATCHED THEN
                UPDATE SET
                    ATTACHEDNETNAME = source.ATTACHEDNETNAME,
                    AVAILABLESIZE = source.AVAILABLESIZE,
                    COMPRESSED = source.COMPRESSED,
                    DRIVETYPE = source.DRIVETYPE,
                    ENCRYPTED = source.ENCRYPTED,
                    FILESYSTEM = source.FILESYSTEM,
                    MOUNT = source.MOUNT,
                    NODEID = source.NODEID,
                    SIZEUNIT = source.SIZEUNIT,
                    TOTALSIZE = source.TOTALSIZE,
                    VOLUMELABEL = source.VOLUMELABEL,
                    CHANGEDATE = source.CHANGEDATE
            WHEN NOT MATCHED THEN
                INSERT (
                    LOGICALDRIVEID,
                    ATTACHEDNETNAME,
                    AVAILABLESIZE,
                    COMPRESSED,
                    DRIVETYPE,
                    ENCRYPTED,
                    FILESYSTEM,
                    MOUNT,
                    NODEID,
                    SIZEUNIT,
                    TOTALSIZE,
                    VOLUMELABEL,
                    CREATEDATE,
                    CHANGEDATE
                )
                VALUES (
                    source.LOGICALDRIVEID,
                    source.ATTACHEDNETNAME,
                    source.AVAILABLESIZE,
                    source.COMPRESSED,
                    source.DRIVETYPE,
                    source.ENCRYPTED,
                    source.FILESYSTEM,
                    source.MOUNT,
                    source.NODEID,
                    source.SIZEUNIT,
                    source.TOTALSIZE,
                    source.VOLUMELABEL,
                    source.CREATEDATE,
                    source.CHANGEDATE
                )
            """;
}
