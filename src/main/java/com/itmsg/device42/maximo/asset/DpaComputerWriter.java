package com.itmsg.device42.maximo.asset;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

import com.itmsg.device42.dto.maximo.asset.DpaComputerUpsert;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DpaComputerWriter {

    public DpaComputerWriter(@Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate) {
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    private static final Logger log = LoggerFactory.getLogger(DpaComputerWriter.class);

    private final JdbcTemplate maximoJdbcTemplate;

    public void write(List<DpaComputerUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_DPA_COMPUTER_QUERY,
                (PreparedStatement statement) -> {
                    for (DpaComputerUpsert computer : data) {
                        try {
                            statement.setLong(1, computer.nodeId());
                            statement.setString(2, computer.biosName());
                            statement.setString(3, computer.biosVersion());
                            statement.setTimestamp(4, toTimestamp(computer.biosDate()));
                            statement.setObject(5, computer.supportsWmi(), Types.INTEGER);
                            statement.setObject(6, computer.biosPnp(), Types.INTEGER);
                            statement.setBigDecimal(7, computer.ramSize());
                            statement.setString(8, computer.ramUnit());
                            statement.setObject(9, computer.smbios(), Types.INTEGER);
                            statement.setTimestamp(10, toTimestamp(computer.createDate()));
                            statement.setTimestamp(11, toTimestamp(computer.changeDate()));
                            statement.setObject(12, computer.numCpuTotal1(), Types.INTEGER);
                            statement.setObject(13, computer.numCoreTotal(), Types.INTEGER);
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error(
                                    "DPA Computer MERGE에 실패했습니다. nodeId={}",
                                    computer.nodeId(),
                                    e
                            );
                        }
                    }
                    return null;
                }
        );
    }

    /** LocalDateTime을 JDBC Timestamp로 변환하며 null은 그대로 유지한다. */
    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private static final String MERGE_DPA_COMPUTER_QUERY = """
            MERGE INTO MAXIMO.DPACOMPUTER AS target
            USING (
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ) AS source (
                NODEID,
                BIOSNAME,
                BIOSVERSION,
                BIOSDATE,
                SUPPORTSWMI,
                BIOSPNP,
                RAMSIZE,
                RAMUNIT,
                SMBIOS,
                CREATEDATE,
                CHANGEDATE,
                NUMCPUTOTAL1,
                NUMCORETOTAL
            )
            ON target.NODEID = source.NODEID
            WHEN MATCHED THEN
                UPDATE SET
                    BIOSNAME = source.BIOSNAME,
                    BIOSVERSION = source.BIOSVERSION,
                    BIOSDATE = source.BIOSDATE,
                    SUPPORTSWMI = source.SUPPORTSWMI,
                    BIOSPNP = source.BIOSPNP,
                    RAMSIZE = source.RAMSIZE,
                    RAMUNIT = source.RAMUNIT,
                    SMBIOS = source.SMBIOS,
                    CHANGEDATE = source.CHANGEDATE,
                    NUMCPUTOTAL1 = source.NUMCPUTOTAL1,
                    NUMCORETOTAL = source.NUMCORETOTAL
            WHEN NOT MATCHED THEN
                INSERT (
                    NODEID,
                    BIOSNAME,
                    BIOSVERSION,
                    BIOSDATE,
                    SUPPORTSWMI,
                    BIOSPNP,
                    RAMSIZE,
                    RAMUNIT,
                    SMBIOS,
                    CREATEDATE,
                    CHANGEDATE,
                    NUMCPUTOTAL1,
                    NUMCORETOTAL
                )
                VALUES (
                    source.NODEID,
                    source.BIOSNAME,
                    source.BIOSVERSION,
                    source.BIOSDATE,
                    source.SUPPORTSWMI,
                    source.BIOSPNP,
                    source.RAMSIZE,
                    source.RAMUNIT,
                    source.SMBIOS,
                    source.CREATEDATE,
                    source.CHANGEDATE,
                    source.NUMCPUTOTAL1,
                    source.NUMCORETOTAL
                )
            """;
}
