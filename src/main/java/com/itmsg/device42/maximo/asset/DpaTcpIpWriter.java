package com.itmsg.device42.maximo.asset;

import com.itmsg.device42.dto.maximo.asset.DpaTcpIpUpsert;
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
public class DpaTcpIpWriter {

    public DpaTcpIpWriter(@Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate) {
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    private static final Logger log = LoggerFactory.getLogger(DpaTcpIpWriter.class);

    private final JdbcTemplate maximoJdbcTemplate;

    public void write(List<DpaTcpIpUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_DPA_TCP_IP_QUERY,
                (PreparedStatement statement) -> {
                    for (DpaTcpIpUpsert tcpIp : data) {
                        try {
                            statement.setString(1, tcpIp.gateway());
                            statement.setString(2, tcpIp.host());
                            statement.setLong(3, tcpIp.nodeId());
                            statement.setString(4, tcpIp.tcpIpAddress());
                            statement.setString(5, tcpIp.tcpIpNetmask());
                            statement.setTimestamp(6, toTimestamp(tcpIp.createDate()));
                            statement.setTimestamp(7, toTimestamp(tcpIp.changeDate()));
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error(
                                    "DPA TCP/IP MERGE에 실패했습니다. nodeId={}, tcpIpAddress={}",
                                    tcpIp.nodeId(),
                                    tcpIp.tcpIpAddress(),
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

    private static final String MERGE_DPA_TCP_IP_QUERY = """
            MERGE INTO MAXIMO.DPATCPIP AS target
            USING (
                VALUES (?, ?, ?, ?, ?, ?, ?)
            ) AS source (
                GATEWAY,
                HOST,
                NODEID,
                TCPIPADDRESS,
                TCPIPNETMASK,
                CREATEDATE,
                CHANGEDATE
            )
            ON target.NODEID = source.NODEID
                AND target.TCPIPADDRESS = source.TCPIPADDRESS
            WHEN MATCHED THEN
                UPDATE SET
                    GATEWAY = source.GATEWAY,
                    HOST = source.HOST,
                    TCPIPNETMASK = source.TCPIPNETMASK,
                    CHANGEDATE = source.CHANGEDATE
            WHEN NOT MATCHED THEN
                INSERT (
                    TCPIPID,
                    GATEWAY,
                    HOST,
                    NODEID,
                    TCPIPADDRESS,
                    TCPIPNETMASK,
                    CREATEDATE,
                    CHANGEDATE
                )
                VALUES (
                    NEXT VALUE FOR MAXIMO.DPATCPIPSEQ,
                    source.GATEWAY,
                    source.HOST,
                    source.NODEID,
                    source.TCPIPADDRESS,
                    source.TCPIPNETMASK,
                    source.CREATEDATE,
                    source.CHANGEDATE
                )
            """;
}
