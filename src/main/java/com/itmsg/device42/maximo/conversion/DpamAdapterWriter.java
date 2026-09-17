package com.itmsg.device42.maximo.conversion;

import com.itmsg.device42.dto.maximo.conversion.DpamAdapterUpsert;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DpamAdapterWriter {

    public DpamAdapterWriter(@Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate) {
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    private static final Logger log = LoggerFactory.getLogger(DpamAdapterWriter.class);

    private final JdbcTemplate maximoJdbcTemplate;

    public void write(List<DpamAdapterUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_QUERY,
                (PreparedStatement statement) -> {
                    for (DpamAdapterUpsert row : data) {
                        try {
                            statement.setString(1, row.adapterName());
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error("어댑터 변환 대상 MERGE에 실패했습니다. name={}", row.adapterName(), e);
                        }
                    }
                    return null;
                }
        );
    }

    private static final String MERGE_QUERY = """
            MERGE INTO MAXIMO.DPAMADAPTER AS target
            USING (
                VALUES (?)
            ) AS source (
                ADAPTERNAME
            )
            ON target.ADAPTERNAME = source.ADAPTERNAME
            WHEN NOT MATCHED THEN
                INSERT (
                    ADAPTERID,
                    ADAPTERNAME,
                    VALIDATED
                )
                VALUES (
                    NEXT VALUE FOR MAXIMO.DPAMADAPTERSEQ,
                    source.ADAPTERNAME,
                    0
                )
            """;
}
