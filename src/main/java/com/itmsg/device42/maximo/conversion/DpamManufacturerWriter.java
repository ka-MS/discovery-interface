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
public class DpamManufacturerWriter {

    public DpamManufacturerWriter(@Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate) {
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    private static final Logger log = LoggerFactory.getLogger(DpamManufacturerWriter.class);

    private final JdbcTemplate maximoJdbcTemplate;

    public void write(List<DpamManufacturerUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_DPAM_MANUFACTURER_QUERY,
                (PreparedStatement statement) -> {
                    for (DpamManufacturerUpsert manufacturer : data) {
                        try {
                            statement.setString(1, manufacturer.manufacturerName());
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error(
                                    "제조업체 변환 대상 MERGE에 실패했습니다. name={}",
                                    manufacturer.manufacturerName(),
                                    e
                            );
                        }
                    }
                    return null;
                }
        );
    }

    private static final String MERGE_DPAM_MANUFACTURER_QUERY = """
            MERGE INTO MAXIMO.DPAMMANUFACTURER AS target
            USING (
                VALUES (?)
            ) AS source (
                MANUFACTURERNAME
            )
            ON target.MANUFACTURERNAME = source.MANUFACTURERNAME
            WHEN NOT MATCHED THEN
                INSERT (
                    MANUFACTURERID,
                    MANUFACTURERNAME,
                    VALIDATED
                )
                VALUES (
                    NEXT VALUE FOR MAXIMO.DPAMMANUFACTURERSEQ,
                    source.MANUFACTURERNAME,
                    0
                )
            """;
}
