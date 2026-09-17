package com.itmsg.device42.maximo.asset;

import com.itmsg.device42.dto.maximo.asset.DpaNetPrinterUpsert;
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
public class DpaNetPrinterWriter {

    public DpaNetPrinterWriter(@Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate) {
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    private static final Logger log = LoggerFactory.getLogger(DpaNetPrinterWriter.class);

    private final JdbcTemplate maximoJdbcTemplate;

    public void write(List<DpaNetPrinterUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_DPA_NET_PRINTER_QUERY,
                (PreparedStatement statement) -> {
                    for (DpaNetPrinterUpsert printer : data) {
                        try {
                            statement.setLong(1, printer.nodeId());
                            statement.setBigDecimal(2, printer.currentRam());
                            statement.setString(3, printer.netMacAddress());
                            statement.setString(4, printer.networkAddress());
                            statement.setObject(5, printer.trayCount(), java.sql.Types.INTEGER);
                            statement.setString(6, printer.ramUnit());
                            statement.setTimestamp(7, toTimestamp(printer.createDate()));
                            statement.setTimestamp(8, toTimestamp(printer.changeDate()));
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error(
                                    "DPA NetPrinter MERGE에 실패했습니다. nodeId={}",
                                    printer.nodeId(),
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

    private static final String MERGE_DPA_NET_PRINTER_QUERY = """
            MERGE INTO MAXIMO.DPANETPRINTER AS target
            USING (
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ) AS source (
                NODEID,
                CURRENTRAM,
                NETMACADDR,
                NETWORKADDRESS,
                NUMBEROFTRAYS,
                RAMUNIT,
                CREATEDATE,
                CHANGEDATE
            )
            ON target.NODEID = source.NODEID
            WHEN MATCHED THEN
                UPDATE SET
                    CURRENTRAM = source.CURRENTRAM,
                    NETMACADDR = source.NETMACADDR,
                    NETWORKADDRESS = source.NETWORKADDRESS,
                    NUMBEROFTRAYS = source.NUMBEROFTRAYS,
                    RAMUNIT = source.RAMUNIT,
                    CHANGEDATE = source.CHANGEDATE
            WHEN NOT MATCHED THEN
                INSERT (
                    NODEID,
                    CURRENTRAM,
                    NETMACADDR,
                    NETWORKADDRESS,
                    NUMBEROFTRAYS,
                    RAMUNIT,
                    CREATEDATE,
                    CHANGEDATE
                )
                VALUES (
                    source.NODEID,
                    source.CURRENTRAM,
                    source.NETMACADDR,
                    source.NETWORKADDRESS,
                    source.NUMBEROFTRAYS,
                    source.RAMUNIT,
                    source.CREATEDATE,
                    source.CHANGEDATE
                )
            """;
}
