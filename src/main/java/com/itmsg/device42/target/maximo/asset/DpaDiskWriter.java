package com.itmsg.device42.target.maximo.asset;

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
public class DpaDiskWriter {

    public DpaDiskWriter(@Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate) {
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    private static final Logger log = LoggerFactory.getLogger(DpaDiskWriter.class);

    private final JdbcTemplate maximoJdbcTemplate;

    public void write(List<DpaDiskUpsert> data) {
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

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

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
