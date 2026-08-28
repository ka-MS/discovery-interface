package com.itmsg.device42.integration.asset;

import com.itmsg.device42.dto.device42.Device42DpaComputerSource;
import com.itmsg.device42.dto.maximo.DpaComputer;
import com.itmsg.device42.config.Device42ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(2)
public class DpaComputerIntegrate implements AssetIntegrationTask {

    private static final Logger log = LoggerFactory.getLogger(DpaComputerIntegrate.class);

    private static final int DEFAULT_BATCH_SIZE = 10;
    private static final DateTimeFormatter US_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter LEGACY_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    private final Device42ConnectionFactory connectionFactory;
    private final JdbcTemplate maximoJdbcTemplate;

    public DpaComputerIntegrate(
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

        if (totalCount <= 0 || batchSize <= 0) {
            return;
        }

        for (long offset = 0; offset < totalCount; offset += batchSize) {
            int limit = (int) Math.min(batchSize, totalCount - offset);
            List<Device42DpaComputerSource> data = getData(offset, limit);

            List<DpaComputer> mappedData = mapData(data);

            putData(mappedData);
        }
    }

    public long getTotalCount() {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(DEVICE_TOTAL_COUNT_QUERY);
             ResultSet resultSet = statement.executeQuery()) {

            return resultSet.next() ? resultSet.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new IllegalStateException("DPA Computer 대상 장비 건수 조회에 실패했습니다.", e);
        }
    }

    public List<Device42DpaComputerSource> getData(long offset, int limit) {
        String query = DEVICE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            List<Device42DpaComputerSource> computers = new ArrayList<>(limit);
            while (resultSet.next()) {
                computers.add(new Device42DpaComputerSource(
                        resultSet.getInt("device_pk"),
                        resultSet.getString("bios_name"),
                        resultSet.getString("bios_version"),
                        resultSet.getString("bios_release_date"),
                        resultSet.getBigDecimal("ram"),
                        resultSet.getString("ram_size_type"),
                        getNullableInteger(resultSet, "total_cpus"),
                        getNullableInteger(resultSet, "core_per_cpu")
                ));
            }
            return computers;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "DPA Computer 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private List<DpaComputer> mapData(List<Device42DpaComputerSource> data) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<DpaComputer> mappedData = new ArrayList<>(data.size());

        for (Device42DpaComputerSource source : data) {
            mappedData.add(new DpaComputer(
                    (long) source.devicePk(),
                    null,
                    null,
                    null,
                    0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    source.biosName(),
                    source.biosVersion(),
                    parseBiosDate(source.biosReleaseDate()),
                    0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    roundRamSize(source.ram()),
                    source.ramSizeType(),
                    0,
                    applyDateTime,
                    applyDateTime,
                    null,
                    null,
                    null,
                    null,
                    source.totalCpus(),
                    null,
                    null,
                    null,
                    calculateTotalCores(source.totalCpus(), source.corePerCpu()),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            ));
        }

        return mappedData;
    }

    public void putData(List<DpaComputer> data) {
        maximoJdbcTemplate.execute(
                MERGE_DPA_COMPUTER_QUERY,
                (PreparedStatement statement) -> {
                    for (DpaComputer computer : data) {
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

    /**
     * 숫자형 조회값을 nullable Integer로 변환한다.
     * 소수이거나 Integer 범위를 벗어난 값은 데이터 오류로 처리한다.
     */
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

    /** RAM 용량을 소수점 둘째 자리까지 반올림한다. */
    private static BigDecimal roundRamSize(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    /** CPU 개수와 CPU당 코어 수를 곱해 전체 코어 수를 계산한다. */
    private static Integer calculateTotalCores(Integer totalCpus, Integer corePerCpu) {
        if (totalCpus == null || corePerCpu == null) {
            return null;
        }
        return Math.multiplyExact(totalCpus, corePerCpu);
    }

    /**
     * Device42에서 관측되는 BIOS 날짜 형식을 LocalDateTime으로 변환한다.
     * 값이 없거나 지원하지 않는 형식이면 null을 반환한다.
     */
    static LocalDateTime parseBiosDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        try {
            return LocalDate.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(normalized, US_DATE_FORMATTER).atStartOfDay();
            } catch (DateTimeParseException ignoredUsFormat) {
                try {
                    return LocalDateTime.parse(normalized, LEGACY_DATE_TIME_FORMATTER);
                } catch (DateTimeParseException invalidFormat) {
                    log.warn("지원하지 않는 BIOS 날짜 형식입니다. value={}", value);
                    return null;
                }
            }
        }
    }

    /** LocalDateTime을 JDBC Timestamp로 변환하며 null은 그대로 유지한다. */
    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private static final String DEVICE_FILTER = """
            d.type IN ('virtual', 'physical')
            AND (
                d.virtualsubtype_id IS NULL
                OR d.virtualsubtype_id <> 15
            )
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype NOT IN ('Network Printer', 'PDU')
            )
            """;

    private static final String DEVICE_TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            FROM view_device_v2 d
            WHERE
            """ + DEVICE_FILTER;

    private static final String DEVICE_QUERY = """
            SELECT
                d.device_pk,
                v.name AS bios_name,
                d.bios_version,
                d.bios_release_date,
                d.ram,
                d.ram_size_type,
                d.total_cpus,
                d.core_per_cpu
            FROM view_device_v2 d
            LEFT JOIN view_vendor_v1 v
                ON v.vendor_pk = d.bios_vendor_fk
            WHERE
            """ + DEVICE_FILTER + """
            ORDER BY d.device_pk
            """;

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
