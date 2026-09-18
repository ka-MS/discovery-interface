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
public class DpaNetAdapterWriter {

    public DpaNetAdapterWriter(@Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate) {
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    private static final Logger log = LoggerFactory.getLogger(DpaNetAdapterWriter.class);

    private final JdbcTemplate maximoJdbcTemplate;

    public void write(List<DpaNetAdapterUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_DPA_NET_ADAPTER_QUERY,
                (PreparedStatement statement) -> {
                    for (DpaNetAdapterUpsert adapter : data) {
                        try {
                            statement.setLong(1, adapter.adapterId());
                            statement.setString(2, adapter.adapterType());
                            statement.setBigDecimal(3, adapter.bandwidth());
                            statement.setString(4, adapter.bandwidthUnit());
                            statement.setString(5, adapter.description());
                            statement.setString(6, adapter.makeModel());
                            statement.setString(7, adapter.manufacturer());
                            statement.setString(8, adapter.netMacAddr1());
                            statement.setString(9, adapter.netMacAddr2());
                            statement.setLong(10, adapter.nodeId());
                            statement.setString(11, adapter.port());
                            statement.setString(12, adapter.protocol());
                            statement.setTimestamp(13, toTimestamp(adapter.createDate()));
                            statement.setTimestamp(14, toTimestamp(adapter.changeDate()));
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error(
                                    "DPA 네트워크 어댑터 MERGE에 실패했습니다. adapterId={}, nodeId={}",
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

    private static final String MERGE_DPA_NET_ADAPTER_QUERY = """
            MERGE INTO MAXIMO.DPANETADAPTER AS target
            USING (
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ) AS source (
                ADAPTERID,
                ADAPTERTYPE,
                BANDWIDTH,
                BANDWIDTHUNIT,
                DESCRIPTION,
                MAKEMODEL,
                MANUFACTURER,
                NETMACADDR1,
                NETMACADDR2,
                NODEID,
                PORT,
                PROTOCOL,
                CREATEDATE,
                CHANGEDATE
            )
            ON target.ADAPTERID = source.ADAPTERID
            WHEN MATCHED THEN
                UPDATE SET
                    ADAPTERTYPE = source.ADAPTERTYPE,
                    BANDWIDTH = source.BANDWIDTH,
                    BANDWIDTHUNIT = source.BANDWIDTHUNIT,
                    DESCRIPTION = source.DESCRIPTION,
                    MAKEMODEL = source.MAKEMODEL,
                    MANUFACTURER = source.MANUFACTURER,
                    NETMACADDR1 = source.NETMACADDR1,
                    NETMACADDR2 = source.NETMACADDR2,
                    NODEID = source.NODEID,
                    PORT = source.PORT,
                    PROTOCOL = source.PROTOCOL,
                    CHANGEDATE = source.CHANGEDATE
            WHEN NOT MATCHED THEN
                INSERT (
                    ADAPTERID,
                    ADAPTERTYPE,
                    BANDWIDTH,
                    BANDWIDTHUNIT,
                    DESCRIPTION,
                    MAKEMODEL,
                    MANUFACTURER,
                    NETMACADDR1,
                    NETMACADDR2,
                    NODEID,
                    PORT,
                    PROTOCOL,
                    CREATEDATE,
                    CHANGEDATE
                )
                VALUES (
                    source.ADAPTERID,
                    source.ADAPTERTYPE,
                    source.BANDWIDTH,
                    source.BANDWIDTHUNIT,
                    source.DESCRIPTION,
                    source.MAKEMODEL,
                    source.MANUFACTURER,
                    source.NETMACADDR1,
                    source.NETMACADDR2,
                    source.NODEID,
                    source.PORT,
                    source.PROTOCOL,
                    source.CREATEDATE,
                    source.CHANGEDATE
                )
            """;
}
