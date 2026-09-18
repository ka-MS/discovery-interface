package com.itmsg.device42.target.maximo.asset;

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
public class DpaMediaAdapterWriter {

    public DpaMediaAdapterWriter(@Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate) {
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    private static final Logger log = LoggerFactory.getLogger(DpaMediaAdapterWriter.class);

    private final JdbcTemplate maximoJdbcTemplate;

    public void write(List<DpaMediaAdapterUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_DPA_MEDIA_ADAPTER_QUERY,
                (PreparedStatement statement) -> {
                    for (DpaMediaAdapterUpsert adapter : data) {
                        try {
                            statement.setLong(1, adapter.adapterId());
                            statement.setString(2, adapter.description());
                            statement.setString(3, adapter.makeModel());
                            statement.setString(4, adapter.manufacturer());
                            statement.setString(5, adapter.mediaType());
                            statement.setLong(6, adapter.nodeId());
                            statement.setString(7, adapter.serialNumber());
                            statement.setTimestamp(8, toTimestamp(adapter.createDate()));
                            statement.setTimestamp(9, toTimestamp(adapter.changeDate()));
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error(
                                    "DPA 미디어 어댑터 MERGE에 실패했습니다. adapterId={}, nodeId={}",
                                    adapter.adapterId(),
                                    adapter.nodeId(),
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

    private static final String MERGE_DPA_MEDIA_ADAPTER_QUERY = """
            MERGE INTO MAXIMO.DPAMEDIAADAPTER AS target
            USING (
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ) AS source (
                ADAPTERID,
                DESCRIPTION,
                MAKEMODEL,
                MANUFACTURER,
                MEDIATYPE,
                NODEID,
                SERIALNUMBER,
                CREATEDATE,
                CHANGEDATE
            )
            ON target.ADAPTERID = source.ADAPTERID
            WHEN MATCHED THEN
                UPDATE SET
                    DESCRIPTION = source.DESCRIPTION,
                    MAKEMODEL = source.MAKEMODEL,
                    MANUFACTURER = source.MANUFACTURER,
                    MEDIATYPE = source.MEDIATYPE,
                    NODEID = source.NODEID,
                    SERIALNUMBER = source.SERIALNUMBER,
                    CHANGEDATE = source.CHANGEDATE
            WHEN NOT MATCHED THEN
                INSERT (
                    ADAPTERID,
                    DESCRIPTION,
                    MAKEMODEL,
                    MANUFACTURER,
                    MEDIATYPE,
                    NODEID,
                    SERIALNUMBER,
                    CREATEDATE,
                    CHANGEDATE
                )
                VALUES (
                    source.ADAPTERID,
                    source.DESCRIPTION,
                    source.MAKEMODEL,
                    source.MANUFACTURER,
                    source.MEDIATYPE,
                    source.NODEID,
                    source.SERIALNUMBER,
                    source.CREATEDATE,
                    source.CHANGEDATE
                )
            """;
}
