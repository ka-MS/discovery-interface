package com.itmsg.device42.maximo.asset;

import com.itmsg.device42.dto.maximo.asset.DpaOsUpsert;
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
public class DpaOsWriter {

    public DpaOsWriter(@Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate) {
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    private static final Logger log = LoggerFactory.getLogger(DpaOsWriter.class);

    private final JdbcTemplate maximoJdbcTemplate;

    public void write(List<DpaOsUpsert> data) {
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

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

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
