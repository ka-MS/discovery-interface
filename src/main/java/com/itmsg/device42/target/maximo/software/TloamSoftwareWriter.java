package com.itmsg.device42.target.maximo.software;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TloamSoftwareWriter {

    public TloamSoftwareWriter(@Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate) {
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    private static final Logger log = LoggerFactory.getLogger(TloamSoftwareWriter.class);

    private final JdbcTemplate maximoJdbcTemplate;

    public void write(List<TloamSoftwareUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_TLOAM_SOFTWARE_QUERY,
                (PreparedStatement statement) -> {
                    for (TloamSoftwareUpsert software : data) {
                        try {
                            statement.setString(1, software.uniqueId());
                            statement.setString(2, software.softwareName());
                            statement.setString(3, software.manufacturer());
                            statement.setString(4, software.version());
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error(
                                    "TLOAM 소프트웨어 카탈로그 MERGE에 실패했습니다. uniqueId={}",
                                    software.uniqueId(),
                                    e
                            );
                        }
                    }
                    return null;
                }
        );
    }

    private static final String MERGE_TLOAM_SOFTWARE_QUERY = """
            MERGE INTO MAXIMO.TLOAMSOFTWARE AS target
            USING (
                VALUES (?, ?, ?, ?)
            ) AS source (
                UNIQUEID,
                SWNAME,
                MANUFACTURER,
                VERSION
            )
            ON target.UNIQUEID = source.UNIQUEID
            WHEN NOT MATCHED THEN
                INSERT (
                    TLOAMSOFTWAREID,
                    UNIQUEID,
                    SWNAME,
                    MANUFACTURER,
                    VERSION,
                    RELEASE,
                    ROLE,
                    ISIPLA,
                    ISPVU,
                    ISSUBCAP,
                    ISDELETED,
                    ISREVIEWED
                )
                VALUES (
                    NEXT VALUE FOR MAXIMO.TLOAMSOFTWARESEQ,
                    source.UNIQUEID,
                    source.SWNAME,
                    source.MANUFACTURER,
                    source.VERSION,
                    NULL,
                    'SOFTWAREPRODUCT',
                    0,
                    0,
                    0,
                    0,
                    0
                )
            """;
}
