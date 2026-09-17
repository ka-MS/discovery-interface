package com.itmsg.device42.maximo.software;

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
public class DpaSoftwareWriter {

    public DpaSoftwareWriter(@Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate) {
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    private static final Logger log = LoggerFactory.getLogger(DpaSoftwareWriter.class);

    private final JdbcTemplate maximoJdbcTemplate;

    public void write(List<DpaSoftwareUpsert> data) {
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

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

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
