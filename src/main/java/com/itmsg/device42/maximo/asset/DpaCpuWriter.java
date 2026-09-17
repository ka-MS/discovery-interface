package com.itmsg.device42.maximo.asset;

import com.itmsg.device42.dto.maximo.asset.DpaCpuUpsert;
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
public class DpaCpuWriter {

    public DpaCpuWriter(@Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate) {
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    private static final Logger log = LoggerFactory.getLogger(DpaCpuWriter.class);

    private final JdbcTemplate maximoJdbcTemplate;

    public void write(List<DpaCpuUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_DPA_CPU_QUERY,
                (PreparedStatement statement) -> {
                    for (DpaCpuUpsert cpu : data) {
                        try {
                            statement.setLong(1, cpu.cpuId());
                            statement.setString(2, cpu.cpuNum());
                            statement.setBigDecimal(3, cpu.currentSpeed());
                            statement.setString(4, cpu.description());
                            statement.setObject(5, cpu.is64BitEnabled(), java.sql.Types.INTEGER);
                            statement.setString(6, cpu.makeModel());
                            statement.setString(7, cpu.manufacturer());
                            statement.setBigDecimal(8, cpu.maxSpeed());
                            statement.setLong(9, cpu.nodeId());
                            statement.setObject(10, cpu.numCore(), java.sql.Types.INTEGER);
                            statement.setString(11, cpu.speedUnit());
                            statement.setTimestamp(12, toTimestamp(cpu.createDate()));
                            statement.setTimestamp(13, toTimestamp(cpu.changeDate()));
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error(
                                    "DPA CPU MERGE에 실패했습니다. cpuId={}, nodeId={}",
                                    cpu.cpuId(),
                                    cpu.nodeId(),
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

    private static final String MERGE_DPA_CPU_QUERY = """
            MERGE INTO MAXIMO.DPACPU AS target
            USING (
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ) AS source (
                CPUID,
                CPUNUM,
                CURRSPEED,
                DESCRIPTION,
                IS64BITEN,
                MAKEMODEL,
                MANUFACTURER,
                MAXSPEED,
                NODEID,
                NUMCORE,
                SPEEDUNIT,
                CREATEDATE,
                CHANGEDATE
            )
            ON target.CPUID = source.CPUID
            WHEN MATCHED THEN
                UPDATE SET
                    CPUNUM = source.CPUNUM,
                    CURRSPEED = source.CURRSPEED,
                    DESCRIPTION = source.DESCRIPTION,
                    IS64BITEN = source.IS64BITEN,
                    MAKEMODEL = source.MAKEMODEL,
                    MANUFACTURER = source.MANUFACTURER,
                    MAXSPEED = source.MAXSPEED,
                    NODEID = source.NODEID,
                    NUMCORE = source.NUMCORE,
                    SPEEDUNIT = source.SPEEDUNIT,
                    CHANGEDATE = source.CHANGEDATE
            WHEN NOT MATCHED THEN
                INSERT (
                    CPUID,
                    CPUNUM,
                    CURRSPEED,
                    DESCRIPTION,
                    IS64BITEN,
                    MAKEMODEL,
                    MANUFACTURER,
                    MAXSPEED,
                    NODEID,
                    NUMCORE,
                    SPEEDUNIT,
                    CREATEDATE,
                    CHANGEDATE
                )
                VALUES (
                    source.CPUID,
                    source.CPUNUM,
                    source.CURRSPEED,
                    source.DESCRIPTION,
                    source.IS64BITEN,
                    source.MAKEMODEL,
                    source.MANUFACTURER,
                    source.MAXSPEED,
                    source.NODEID,
                    source.NUMCORE,
                    source.SPEEDUNIT,
                    source.CREATEDATE,
                    source.CHANGEDATE
                )
            """;
}
