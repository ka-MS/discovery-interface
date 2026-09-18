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
public class DpamOsVariantWriter {

    public DpamOsVariantWriter(@Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate) {
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    private static final Logger log = LoggerFactory.getLogger(DpamOsVariantWriter.class);

    private final JdbcTemplate maximoJdbcTemplate;

    public void write(List<DpamOsVariantUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_QUERY,
                (PreparedStatement statement) -> {
                    for (DpamOsVariantUpsert row : data) {
                        try {
                            statement.setString(1, row.osName());
                            statement.setString(2, row.osVariant());
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error(
                                    "운영체제 변환 변형 MERGE에 실패했습니다. name={}, variant={}",
                                    row.osName(),
                                    row.osVariant(),
                                    e
                            );
                        }
                    }
                    return null;
                }
        );
    }

    private static final String MERGE_QUERY = """
            MERGE INTO MAXIMO.DPAMOSVARIANT AS target
            USING (
                VALUES (?, ?)
            ) AS source (
                OSNAME,
                OSVARIANT
            )
            ON target.OSVARIANT = source.OSVARIANT
            WHEN NOT MATCHED THEN
                INSERT (
                    DPAMOSVARIANTID,
                    OSNAME,
                    OSVARIANT
                )
                VALUES (
                    NEXT VALUE FOR MAXIMO.DPAMOSVARIANTSEQ,
                    source.OSNAME,
                    source.OSVARIANT
                )
            """;
}
