package com.itmsg.device42.integration.asset;

import com.itmsg.device42.dto.device42.Device42DpaCpuSource;
import com.itmsg.device42.dto.maximo.DpaCpuUpsert;
import com.itmsg.device42.integration.config.Device42ConnectionFactory;
import com.itmsg.device42.integration.maximo.SourceTargetMapKey;
import com.itmsg.device42.integration.maximo.SourceTargetMapRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Order(5)
public class DpaCpuIntegrate implements AssetIntegrationTask {

    private static final Logger log = LoggerFactory.getLogger(DpaCpuIntegrate.class);

    private static final int DEFAULT_BATCH_SIZE = 10;
    private static final String IMPORT_SOURCE = "Device42";
    private static final String SOURCE_SYSTEM = "DEVICE42";
    private static final String SOURCE_OBJECT = "view_part_v1";
    private static final String TARGET_SYSTEM = "MAXIMO";
    private static final String TARGET_OBJECT = "DPACPU";
    private static final String UNKNOWN = "UNKNOWN";
    private static final BigDecimal ZERO_SPEED = new BigDecimal("0.00");

    private final Device42ConnectionFactory connectionFactory;
    private final JdbcTemplate maximoJdbcTemplate;
    private final SourceTargetMapRepository sourceTargetMapRepository;
    private final TransactionTemplate rowTransaction;

    public DpaCpuIntegrate(
            Device42ConnectionFactory connectionFactory,
            @Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate,
            SourceTargetMapRepository sourceTargetMapRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.connectionFactory = connectionFactory;
        this.maximoJdbcTemplate = maximoJdbcTemplate;
        this.sourceTargetMapRepository = sourceTargetMapRepository;
        this.rowTransaction = new TransactionTemplate(transactionManager);
        this.rowTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void integrate() {
        long totalCount = getTotalCount();
        if (totalCount <= 0) {
            return;
        }

        for (long offset = 0; offset < totalCount; offset += DEFAULT_BATCH_SIZE) {
            int limit = (int) Math.min(DEFAULT_BATCH_SIZE, totalCount - offset);
            List<Device42DpaCpuSource> sourceRows = getData(offset, limit);
            if (sourceRows.isEmpty()) {
                continue;
            }

            putData(mapData(sourceRows));
        }
    }

    public long getTotalCount() {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(TOTAL_COUNT_QUERY);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new IllegalStateException("DPA CPU 대상 파트 건수 조회에 실패했습니다.", e);
        }
    }

    public List<Device42DpaCpuSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            List<Device42DpaCpuSource> rows = new ArrayList<>(limit);
            while (resultSet.next()) {
                rows.add(new Device42DpaCpuSource(
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

    List<DpaCpuUpsert> mapData(List<Device42DpaCpuSource> sourceRows) {
        Map<Long, Long> nodeIds = getNodeIds(sourceRows);
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<DpaCpuUpsert> mappedRows = new ArrayList<>(sourceRows.size());

        for (Device42DpaCpuSource source : sourceRows) {
            Long nodeId = nodeIds.get(source.deviceFk());
            if (nodeId == null) {
                log.warn(
                        "DEPLOYEDASSET 교차키가 없어 DPA CPU 적재를 건너뜁니다. partPk={}, deviceFk={}",
                        source.partPk(),
                        source.deviceFk()
                );
                continue;
            }

            mappedRows.add(new DpaCpuUpsert(
                    source.partPk(),
                    source.deviceFk(),
                    nodeId,
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
        return mappedRows;
    }

    private Map<Long, Long> getNodeIds(List<Device42DpaCpuSource> sourceRows) {
        Set<Long> deviceIds = new LinkedHashSet<>();
        for (Device42DpaCpuSource source : sourceRows) {
            deviceIds.add(source.deviceFk());
        }
        if (deviceIds.isEmpty()) {
            return Map.of();
        }

        String placeholders = String.join(", ", Collections.nCopies(deviceIds.size(), "?"));
        String query = DEPLOYED_ASSET_NODE_ID_QUERY.formatted(placeholders);

        return maximoJdbcTemplate.query(
                query,
                statement -> {
                    statement.setString(1, IMPORT_SOURCE);
                    int parameterIndex = 2;
                    for (Long deviceId : deviceIds) {
                        statement.setString(parameterIndex++, String.valueOf(deviceId));
                    }
                },
                resultSet -> {
                    Map<Long, Long> result = new HashMap<>();
                    while (resultSet.next()) {
                        result.put(
                                Long.valueOf(resultSet.getString("sourceid")),
                                resultSet.getLong("nodeid")
                        );
                    }
                    return result;
                }
        );
    }

    public void putData(List<DpaCpuUpsert> rows) {
        for (DpaCpuUpsert row : rows) {
            try {
                rowTransaction.executeWithoutResult(status -> synchronizeRow(row));
            } catch (RuntimeException e) {
                log.error(
                        "DPA CPU 적재에 실패하여 해당 원천 행을 롤백했습니다. partPk={}, deviceFk={}",
                        row.sourceId(),
                        row.sourceParentId(),
                        e
                );
            }
        }
    }

    private void synchronizeRow(DpaCpuUpsert row) {
        SourceTargetMapKey mapKey = new SourceTargetMapKey(
                SOURCE_SYSTEM,
                SOURCE_OBJECT,
                row.sourceId(),
                TARGET_SYSTEM,
                TARGET_OBJECT
        );
        Long targetId = sourceTargetMapRepository.findTargetId(mapKey);
        boolean newMapping = targetId == null;

        if (newMapping) {
            targetId = maximoJdbcTemplate.queryForObject(NEXT_CPU_ID_QUERY, Long.class);
            if (targetId == null) {
                throw new IllegalStateException("MAXIMO.DPACPUSEQ가 ID를 반환하지 않았습니다.");
            }
        }

        mergeTarget(row, targetId);

        if (newMapping) {
            sourceTargetMapRepository.insert(
                    mapKey,
                    row.sourceParentId(),
                    targetId,
                    row.nodeId(),
                    row.changeDate()
            );
            return;
        }

        sourceTargetMapRepository.updateParents(
                mapKey,
                row.sourceParentId(),
                row.nodeId(),
                row.changeDate()
        );
    }

    private void mergeTarget(DpaCpuUpsert row, Long targetId) {
        int updatedRows = maximoJdbcTemplate.update(
                MERGE_TARGET_QUERY,
                targetId,
                row.cpuNum(),
                row.currentSpeed(),
                row.description(),
                row.is64BitEnabled(),
                row.makeModel(),
                row.manufacturer(),
                row.maxSpeed(),
                row.nodeId(),
                row.numCore(),
                row.speedUnit(),
                Timestamp.valueOf(row.createDate()),
                Timestamp.valueOf(row.changeDate())
        );
        if (updatedRows != 1) {
            throw new IllegalStateException(
                    "MAXIMO.DPACPU MERGE 결과가 1건이 아닙니다. cpuId=%d, rows=%d"
                            .formatted(targetId, updatedRows)
            );
        }
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

    private static final String DEPLOYED_ASSET_NODE_ID_QUERY = """
            SELECT nodeid, sourceid
            FROM MAXIMO.DEPLOYEDASSET
            WHERE importsource = ?
              AND sourceid IN (%s)
            """;

    private static final String NEXT_CPU_ID_QUERY =
            "VALUES NEXT VALUE FOR MAXIMO.DPACPUSEQ";

    private static final String MERGE_TARGET_QUERY = """
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
