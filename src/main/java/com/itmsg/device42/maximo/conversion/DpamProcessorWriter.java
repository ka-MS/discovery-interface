package com.itmsg.device42.maximo.conversion;

import com.itmsg.device42.dto.maximo.conversion.DpamProcessorUpsert;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DpamProcessorWriter {

    public DpamProcessorWriter(@Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate) {
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    private static final Logger log = LoggerFactory.getLogger(DpamProcessorWriter.class);

    private final JdbcTemplate maximoJdbcTemplate;

    public void write(List<DpamProcessorUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_QUERY,
                (PreparedStatement statement) -> {
                    for (DpamProcessorUpsert row : data) {
                        try {
                            statement.setString(1, row.processorName());
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error("프로세서 변환 대상 MERGE에 실패했습니다. name={}", row.processorName(), e);
                        }
                    }
                    return null;
                }
        );
    }

    private static final String MERGE_QUERY = """
            MERGE INTO MAXIMO.DPAMPROCESSOR AS target
            USING (
                VALUES (?)
            ) AS source (
                PROCESSORNAME
            )
            ON target.PROCESSORNAME = source.PROCESSORNAME
            WHEN NOT MATCHED THEN
                INSERT (
                    PROCESSORID,
                    PROCESSORNAME,
                    VALIDATED
                )
                VALUES (
                    NEXT VALUE FOR MAXIMO.DPAMPROCESSORSEQ,
                    source.PROCESSORNAME,
                    0
                )
            """;
}
