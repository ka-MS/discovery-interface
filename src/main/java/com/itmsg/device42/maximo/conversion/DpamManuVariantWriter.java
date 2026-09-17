package com.itmsg.device42.maximo.conversion;

import com.itmsg.device42.dto.maximo.conversion.DpamManuVariantUpsert;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DpamManuVariantWriter {

    public DpamManuVariantWriter(@Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate) {
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    private static final Logger log = LoggerFactory.getLogger(DpamManuVariantWriter.class);

    private final JdbcTemplate maximoJdbcTemplate;

    public void write(List<DpamManuVariantUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_DPAM_MANU_VARIANT_QUERY,
                (PreparedStatement statement) -> {
                    for (DpamManuVariantUpsert variant : data) {
                        try {
                            statement.setString(1, variant.manufacturerName());
                            statement.setString(2, variant.manufacturerVar());
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error(
                                    "제조업체 변환 변형 MERGE에 실패했습니다. name={}, variant={}",
                                    variant.manufacturerName(),
                                    variant.manufacturerVar(),
                                    e
                            );
                        }
                    }
                    return null;
                }
        );
    }

    private static final String MERGE_DPAM_MANU_VARIANT_QUERY = """
            MERGE INTO MAXIMO.DPAMMANUVARIANT AS target
            USING (
                VALUES (?, ?)
            ) AS source (
                MANUFACTURERNAME,
                MANUFACTURERVAR
            )
            ON target.MANUFACTURERVAR = source.MANUFACTURERVAR
            WHEN NOT MATCHED THEN
                INSERT (
                    DPAMMANUVARIANTID,
                    MANUFACTURERNAME,
                    MANUFACTURERVAR
                )
                VALUES (
                    NEXT VALUE FOR MAXIMO.DPAMMANUVARIANTSEQ,
                    source.MANUFACTURERNAME,
                    source.MANUFACTURERVAR
                )
            """;
}
