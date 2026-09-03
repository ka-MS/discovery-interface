package com.itmsg.device42.integration.asset;

import com.itmsg.device42.dto.device42.asset.ProcessorSource;
import com.itmsg.device42.dto.maximo.asset.DpaCpuUpsert;
import com.itmsg.device42.config.Device42ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(5)
public class DpaCpuIntegrate implements AssetIntegrationTask {

    private static final Logger log = LoggerFactory.getLogger(DpaCpuIntegrate.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final String UNKNOWN = "UNKNOWN";
    private static final BigDecimal ZERO_SPEED = new BigDecimal("0.00");

    private final Device42ConnectionFactory connectionFactory;
    private final JdbcTemplate maximoJdbcTemplate;

    public DpaCpuIntegrate(
            Device42ConnectionFactory connectionFactory,
            @Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate
    ) {
        this.connectionFactory = connectionFactory;
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    @Override
    public void integrate() {
        long totalCount = getTotalCount();
        int batchSize = DEFAULT_BATCH_SIZE;

        if (totalCount <= 0) {
            log.info("배치할 DPA CPU 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        for (long offset = 0; offset < totalCount; offset += batchSize) {
            int limit = (int) Math.min(batchSize, totalCount - offset);

            List<ProcessorSource> data = getData(offset, limit);

            List<DpaCpuUpsert> mappedData = mapData(data);

            putData(mappedData);
        }
    }

    public long getTotalCount() {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(TOTAL_COUNT_QUERY);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return resultSet.getLong(1);
            }

            return 0L;
        } catch (SQLException e) {
            throw new IllegalStateException("DPA CPU 대상 파트 건수 조회에 실패했습니다.", e);
        }
    }

    public List<ProcessorSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            List<ProcessorSource> rows = new ArrayList<>(limit);

            while (resultSet.next()) {
                rows.add(new ProcessorSource(
                        resultSet.getLong("part_pk"),
                        resultSet.getLong("device_fk"),
                        resultSet.getString("slot"),
                        resultSet.getString("description"),
                        resultSet.getString("model_name"),
                        getNullableInteger(resultSet, "cores"),
                        resultSet.getBigDecimal("speed"),
                        resultSet.getString("speed_unit"),
                        resultSet.getString("vendor_name")
                ));
            }

            return rows;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "DPA CPU 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private List<DpaCpuUpsert> mapData(List<ProcessorSource> data) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<DpaCpuUpsert> mappedData = new ArrayList<>(data.size());

        for (ProcessorSource source : data) {
            mappedData.add(new DpaCpuUpsert(
                    source.partPk(),
                    source.deviceFk(),
                    trimToNull(source.slot()),
                    ZERO_SPEED,
                    firstNonBlank(source.description(), source.modelName()),
                    0,
                    defaultUnknown(source.modelName()),
                    defaultUnknown(source.vendorName()),
                    roundSpeed(source.speed()),
                    source.cores(),
                    trimToNull(source.speedUnit()),
                    applyDateTime,
                    applyDateTime
            ));
        }

        return mappedData;
    }

    public void putData(List<DpaCpuUpsert> data) {
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

    private static Integer getNullableInteger(ResultSet resultSet, String column) throws SQLException {
        BigDecimal value = resultSet.getBigDecimal(column);
        if (value == null) {
            return null;
        }
        try {
            return value.intValueExact();
        } catch (ArithmeticException e) {
            throw new SQLException(column + " 값을 정수로 변환할 수 없습니다: " + value, e);
        }
    }

    static BigDecimal roundSpeed(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    static String firstNonBlank(String primary, String fallback) {
        String normalizedPrimary = trimToNull(primary);
        return normalizedPrimary == null ? trimToNull(fallback) : normalizedPrimary;
    }

    static String defaultUnknown(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? UNKNOWN : normalized;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private static final String DEVICE_FILTER = """
            d.type IN ('virtual', 'physical')
            AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype NOT IN ('Network Printer', 'PDU')
            )
            """;

    private static final String SOURCE_FROM_AND_FILTER = """
            FROM view_part_v1 p
            JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
            JOIN view_device_v2 d ON d.device_pk = p.device_fk
            LEFT JOIN view_vendor_v1 v ON v.vendor_pk = pm.vendor_fk
            WHERE pm.type_name = 'CPU'
              AND
            """ + DEVICE_FILTER;

    private static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            """ + SOURCE_FROM_AND_FILTER;

    private static final String SOURCE_QUERY = """
            SELECT
                p.part_pk,
                p.device_fk,
                p.slot,
                p.description,
                pm.name AS model_name,
                pm.cores,
                pm.speed,
                pm.speed_unit,
                v.name AS vendor_name
            """ + SOURCE_FROM_AND_FILTER + """
            ORDER BY p.device_fk, p.slot, p.part_pk
            """;

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
