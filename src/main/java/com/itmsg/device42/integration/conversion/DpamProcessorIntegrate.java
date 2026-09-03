package com.itmsg.device42.integration.conversion;

import com.itmsg.device42.dto.device42.conversion.ProcessorModelSource;
import com.itmsg.device42.dto.maximo.conversion.DpamProcessorUpsert;
import com.itmsg.device42.config.Device42ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(5)
public class DpamProcessorIntegrate implements ConversionIntegrationTask {

    private static final Logger log = LoggerFactory.getLogger(DpamProcessorIntegrate.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final Device42ConnectionFactory connectionFactory;
    private final JdbcTemplate maximoJdbcTemplate;

    public DpamProcessorIntegrate(
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
            log.info("배치할 프로세서 변환 대상 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        for (long offset = 0; offset < totalCount; offset += batchSize) {
            int limit = (int) Math.min(batchSize, totalCount - offset);

            List<ProcessorModelSource> data = getData(offset, limit);

            List<DpamProcessorUpsert> mappedData = mapData(data);

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
            throw new IllegalStateException("프로세서 변환 대상 건수 조회에 실패했습니다.", e);
        }
    }

    public List<ProcessorModelSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            List<ProcessorModelSource> rows = new ArrayList<>(limit);

            while (resultSet.next()) {
                rows.add(new ProcessorModelSource(resultSet.getString("name")));
            }

            return rows;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "프로세서 변환 대상 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private List<DpamProcessorUpsert> mapData(List<ProcessorModelSource> data) {
        List<DpamProcessorUpsert> mappedData = new ArrayList<>(data.size());

        for (ProcessorModelSource source : data) {
            String name = trimToNull(source.modelName());
            if (name != null) {
                mappedData.add(new DpamProcessorUpsert(name));
            }
        }

        return mappedData;
    }

    public void putData(List<DpamProcessorUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_QUERY,
                (PreparedStatement statement) -> {
                    for (DpamProcessorUpsert row : data) {
                        try {
                            statement.setString(1, row.processorName());
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error("프로세서 변환 대상 MERGE에 실패했습니다. name={}", row.processorName(), e);
                        }
                    }
                    return null;
                }
        );
    }


    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static final String SOURCE_QUERY = """
            SELECT DISTINCT pm.name
            FROM view_part_v1 p
            JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
            JOIN view_device_v2 d ON d.device_pk = p.device_fk
            WHERE pm.type_name = 'CPU'
              AND
            d.type IN ('virtual', 'physical')
            AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype NOT IN ('Network Printer', 'PDU')
            )
              AND pm.name IS NOT NULL
              AND pm.name <> ''
            """;

    private static final String TOTAL_COUNT_QUERY = """
            WITH source AS (
""" + SOURCE_QUERY + """
            )
            SELECT COUNT(*) FROM source
            """;

    private static final String MERGE_QUERY = """
            MERGE INTO MAXIMO.DPAMPROCESSOR AS target
            USING (
                VALUES (?)
            ) AS source (
                PROCESSORNAME
            )
            ON target.PROCESSORNAME = source.PROCESSORNAME
            WHEN NOT MATCHED THEN
                INSERT (
                    PROCESSORID,
                    PROCESSORNAME,
                    VALIDATED
                )
                VALUES (
                    NEXT VALUE FOR MAXIMO.DPAMPROCESSORSEQ,
                    source.PROCESSORNAME,
                    0
                )
            """;

}
