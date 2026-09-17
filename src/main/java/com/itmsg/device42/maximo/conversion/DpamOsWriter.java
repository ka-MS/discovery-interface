package com.itmsg.device42.maximo.conversion;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DpamOsWriter {

    public DpamOsWriter(@Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate) {
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    private static final Logger log = LoggerFactory.getLogger(DpamOsWriter.class);

    private final JdbcTemplate maximoJdbcTemplate;

    public void write(List<DpamOsUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_QUERY,
                (PreparedStatement statement) -> {
                    for (DpamOsUpsert row : data) {
                        try {
                            statement.setString(1, row.osName());
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error("운영체제 변환 대상 MERGE에 실패했습니다. name={}", row.osName(), e);
                        }
                    }
                    return null;
                }
        );
    }

    private static final String MERGE_QUERY = """
            MERGE INTO MAXIMO.DPAMOS AS target
            USING (
                VALUES (?)
            ) AS source (
                OSNAME
            )
            ON target.OSNAME = source.OSNAME
            WHEN NOT MATCHED THEN
                INSERT (
                    OSID,
                    OSNAME,
                    VALIDATED
                )
                VALUES (
                    NEXT VALUE FOR MAXIMO.DPAMOSSEQ,
                    source.OSNAME,
                    0
                )
            """;
}
