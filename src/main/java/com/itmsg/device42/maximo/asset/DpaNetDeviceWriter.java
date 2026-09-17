package com.itmsg.device42.maximo.asset;

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
public class DpaNetDeviceWriter {

    public DpaNetDeviceWriter(@Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate) {
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    private static final Logger log = LoggerFactory.getLogger(DpaNetDeviceWriter.class);

    private final JdbcTemplate maximoJdbcTemplate;

    public void write(List<DpaNetDeviceUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_DPA_NET_DEVICE_QUERY,
                (PreparedStatement statement) -> {
                    for (DpaNetDeviceUpsert device : data) {
                        try {
                            statement.setLong(1, device.nodeId());
                            statement.setString(2, device.netMacAddress());
                            statement.setString(3, device.networkAddress());
                            statement.setString(4, device.osVersion());
                            statement.setTimestamp(5, toTimestamp(device.createDate()));
                            statement.setTimestamp(6, toTimestamp(device.changeDate()));
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error(
                                    "DPA NetDevice MERGE에 실패했습니다. nodeId={}",
                                    device.nodeId(),
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

    private static final String MERGE_DPA_NET_DEVICE_QUERY = """
            MERGE INTO MAXIMO.DPANETDEVICE AS target
            USING (
                VALUES (?, ?, ?, ?, ?, ?)
            ) AS source (
                NODEID,
                NETMACADDR,
                NETWORKADDRESS,
                OSVERSION,
                CREATEDATE,
                CHANGEDATE
            )
            ON target.NODEID = source.NODEID
            WHEN MATCHED THEN
                UPDATE SET
                    NETMACADDR = source.NETMACADDR,
                    NETWORKADDRESS = source.NETWORKADDRESS,
                    OSVERSION = source.OSVERSION,
                    CHANGEDATE = source.CHANGEDATE
            WHEN NOT MATCHED THEN
                INSERT (
                    NODEID,
                    NETMACADDR,
                    NETWORKADDRESS,
                    OSVERSION,
                    CREATEDATE,
                    CHANGEDATE
                )
                VALUES (
                    source.NODEID,
                    source.NETMACADDR,
                    source.NETWORKADDRESS,
                    source.OSVERSION,
                    source.CREATEDATE,
                    source.CHANGEDATE
                )
            """;
}
