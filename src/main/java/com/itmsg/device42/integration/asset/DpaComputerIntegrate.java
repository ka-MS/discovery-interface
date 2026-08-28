package com.itmsg.device42.integration.asset;

import com.itmsg.device42.dto.device42.Device42DpaComputerSource;
import com.itmsg.device42.dto.maximo.DpaComputer;
import com.itmsg.device42.integration.config.Device42ConnectionFactory;
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
import java.util.*;

@Component
@Order(2)
public class DpaComputerIntegrate implements AssetIntegrationTask {

    private static final Logger log = LoggerFactory.getLogger(DpaComputerIntegrate.class);

    private static final int DEFAULT_BATCH_SIZE = 10;
    private static final String IMPORT_SOURCE = "Device42";
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
            int limit = (int) Math.min((long) batchSize, totalCount - offset);
            List<Device42DpaComputerSource> data = getData(offset, limit);

            if (data.isEmpty()) {
                continue;
            }

            List<DpaComputer> mappedData = mapData(data);
            if (mappedData.isEmpty()) {
                continue;
            }

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
        Map<String, Long> nodeIds = getNodeIds(data);
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<DpaComputer> mappedData = new ArrayList<>(data.size());

        for (Device42DpaComputerSource source : data) {
            String sourceId = String.valueOf(source.devicePk());
            Long nodeId = nodeIds.get(sourceId);
            if (nodeId == null) {
                log.warn(
                        "DEPLOYEDASSET 교차키가 없어 DPA Computer 적재를 건너뜁니다. sourceId={}",
                        sourceId
                );
                continue;
            }

            mappedData.add(new DpaComputer(
                    nodeId,
                    null,
                    null,
                    null,
                    0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
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

    private Map<String, Long> getNodeIds(List<Device42DpaComputerSource> data) {
        if (data.isEmpty()) {
            return Map.of();
        }

        String placeholders = String.join(", ", Collections.nCopies(data.size(), "?"));
        String query = DEPLOYED_ASSET_NODE_ID_QUERY.formatted(placeholders);

        return maximoJdbcTemplate.query(
                query,
                statement -> {
                    statement.setString(1, IMPORT_SOURCE);
                    for (int index = 0; index < data.size(); index++) {
                        statement.setString(index + 2, String.valueOf(data.get(index).devicePk()));
                    }
                },
                resultSet -> {
                    Map<String, Long> nodeIds = new HashMap<>();
                    while (resultSet.next()) {
                        nodeIds.put(
                                resultSet.getString("sourceid"),
                                resultSet.getLong("nodeid")
                        );
                    }
                    return nodeIds;
                }
        );
    }

    public void putData(List<DpaComputer> data) {
        maximoJdbcTemplate.execute(
                MERGE_DPA_COMPUTER_QUERY,
                (PreparedStatement statement) -> {
                    for (DpaComputer computer : data) {
                        try {
                            statement.setLong(1, computer.nodeId());
                            statement.setString(2, computer.biosVersion());
                            statement.setTimestamp(3, toTimestamp(computer.biosDate()));
                            statement.setObject(4, computer.supportsWmi(), Types.INTEGER);
                            statement.setObject(5, computer.biosPnp(), Types.INTEGER);
                            statement.setBigDecimal(6, computer.ramSize());
                            statement.setString(7, computer.ramUnit());
                            statement.setObject(8, computer.smbios(), Types.INTEGER);
                            statement.setTimestamp(9, toTimestamp(computer.createDate()));
                            statement.setTimestamp(10, toTimestamp(computer.changeDate()));
                            statement.setObject(11, computer.numCpuTotal1(), Types.INTEGER);
                            statement.setObject(12, computer.numCoreTotal(), Types.INTEGER);
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

    private static BigDecimal roundRamSize(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private static Integer calculateTotalCores(Integer totalCpus, Integer corePerCpu) {
        if (totalCpus == null || corePerCpu == null) {
            return null;
        }
        return Math.multiplyExact(totalCpus, corePerCpu);
    }

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
                d.bios_version,
                d.bios_release_date,
                d.ram,
                d.ram_size_type,
                d.total_cpus,
                d.core_per_cpu
            FROM view_device_v2 d
            WHERE
            """ + DEVICE_FILTER + """
            ORDER BY d.device_pk
            """;

    private static final String DEPLOYED_ASSET_NODE_ID_QUERY = """
            SELECT nodeid, sourceid
            FROM MAXIMO.DEPLOYEDASSET
            WHERE importsource = ?
              AND sourceid IN (%s)
            """;

    private static final String MERGE_DPA_COMPUTER_QUERY = """
            MERGE INTO MAXIMO.DPACOMPUTER AS target
            USING (
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ) AS source (
                NODEID,
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
