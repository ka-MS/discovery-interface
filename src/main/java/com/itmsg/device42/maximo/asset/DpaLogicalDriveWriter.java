package com.itmsg.device42.maximo.asset;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DpaLogicalDriveWriter {

    public DpaLogicalDriveWriter(@Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate) {
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    private static final Logger log = LoggerFactory.getLogger(DpaLogicalDriveWriter.class);

    private final JdbcTemplate maximoJdbcTemplate;

    public void write(List<DpaLogicalDriveUpsert> data) {
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

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

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
