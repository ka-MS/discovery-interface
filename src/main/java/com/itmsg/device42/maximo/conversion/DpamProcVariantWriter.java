package com.itmsg.device42.maximo.conversion;

import com.itmsg.device42.dto.maximo.conversion.DpamProcVariantUpsert;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DpamProcVariantWriter {

    public DpamProcVariantWriter(@Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate) {
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    private static final Logger log = LoggerFactory.getLogger(DpamProcVariantWriter.class);

    private final JdbcTemplate maximoJdbcTemplate;

    public void write(List<DpamProcVariantUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_QUERY,
                (PreparedStatement statement) -> {
                    for (DpamProcVariantUpsert row : data) {
                        try {
                            statement.setString(1, row.processorName());
                            statement.setString(2, row.processorVar());
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error(
                                    "프로세서 변환 변형 MERGE에 실패했습니다. name={}, variant={}",
                                    row.processorName(),
                                    row.processorVar(),
                                    e
                            );
                        }
                    }
                    return null;
                }
        );
    }

    private static final String MERGE_QUERY = """
            MERGE INTO MAXIMO.DPAMPROCVARIANT AS target
            USING (
                VALUES (?, ?)
            ) AS source (
                PROCESSORNAME,
                PROCESSORVAR
            )
            ON target.PROCESSORVAR = source.PROCESSORVAR
            WHEN NOT MATCHED THEN
                INSERT (
                    DPAMPROCVARIANTID,
                    PROCESSORNAME,
                    PROCESSORVAR
                )
                VALUES (
                    NEXT VALUE FOR MAXIMO.DPAMPROCVARIANTSEQ,
                    source.PROCESSORNAME,
                    source.PROCESSORVAR
                )
            """;
}
