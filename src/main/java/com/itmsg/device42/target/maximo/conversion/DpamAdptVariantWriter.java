package com.itmsg.device42.target.maximo.conversion;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DpamAdptVariantWriter {

    public DpamAdptVariantWriter(@Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate) {
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    private static final Logger log = LoggerFactory.getLogger(DpamAdptVariantWriter.class);

    private final JdbcTemplate maximoJdbcTemplate;

    public void write(List<DpamAdptVariantUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_QUERY,
                (PreparedStatement statement) -> {
                    for (DpamAdptVariantUpsert row : data) {
                        try {
                            statement.setString(1, row.adapterName());
                            statement.setString(2, row.adapterVariant());
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error(
                                    "어댑터 변환 변형 MERGE에 실패했습니다. name={}, variant={}",
                                    row.adapterName(),
                                    row.adapterVariant(),
                                    e
                            );
                        }
                    }
                    return null;
                }
        );
    }

    private static final String MERGE_QUERY = """
            MERGE INTO MAXIMO.DPAMADPTVARIANT AS target
            USING (
                VALUES (?, ?)
            ) AS source (
                ADAPTERNAME,
                ADAPTERVARIANT
            )
            ON target.ADAPTERVARIANT = source.ADAPTERVARIANT
            WHEN NOT MATCHED THEN
                INSERT (
                    DPAMADPTVARIANTID,
                    ADAPTERNAME,
                    ADAPTERVARIANT
                )
                VALUES (
                    NEXT VALUE FOR MAXIMO.DPAMADPTVARIANTSEQ,
                    source.ADAPTERNAME,
                    source.ADAPTERVARIANT
                )
            """;
}
