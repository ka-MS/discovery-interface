package com.itmsg.device42.integration.conversion;

import com.itmsg.device42.dto.device42.conversion.AdapterModelSource;
import com.itmsg.device42.dto.maximo.conversion.DpamAdptVariantUpsert;
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
@Order(8)
public class DpamAdptVariantIntegrate implements ConversionIntegrationTask {

    private static final Logger log = LoggerFactory.getLogger(DpamAdptVariantIntegrate.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final Device42ConnectionFactory connectionFactory;
    private final JdbcTemplate maximoJdbcTemplate;

    public DpamAdptVariantIntegrate(
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
            log.info("배치할 어댑터 변환 변형 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        for (long offset = 0; offset < totalCount; offset += batchSize) {
            int limit = (int) Math.min(batchSize, totalCount - offset);

            List<AdapterModelSource> data = getData(offset, limit);

            List<DpamAdptVariantUpsert> mappedData = mapData(data);

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
            throw new IllegalStateException("어댑터 변환 변형 건수 조회에 실패했습니다.", e);
        }
    }

    public List<AdapterModelSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            List<AdapterModelSource> rows = new ArrayList<>(limit);

            while (resultSet.next()) {
                rows.add(new AdapterModelSource(resultSet.getString("name")));
            }

            return rows;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "어댑터 변환 변형 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private List<DpamAdptVariantUpsert> mapData(List<AdapterModelSource> data) {
        List<DpamAdptVariantUpsert> mappedData = new ArrayList<>(data.size());

        for (AdapterModelSource source : data) {
            String variant = trimToNull(source.modelName());
            if (variant != null) {
                mappedData.add(new DpamAdptVariantUpsert(variant, variant));
            }
        }

        return mappedData;
    }

    public void putData(List<DpamAdptVariantUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_QUERY,
                (PreparedStatement statement) -> {
                    for (DpamAdptVariantUpsert row : data) {
                        try {
                            statement.setString(1, row.adapterName());
                            statement.setString(2, row.adapterVariant());
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error(
                                    "어댑터 변환 변형 MERGE에 실패했습니다. name={}, variant={}",
                                    row.adapterName(),
                                    row.adapterVariant(),
                                    e
                            );
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
            WITH gpu AS (
                SELECT DISTINCT pm.name
                FROM view_part_v1 p
                JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
                JOIN view_device_v2 d ON d.device_pk = p.device_fk
                WHERE pm.type_name = 'GPU'
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
            )
            SELECT name FROM gpu
            UNION
            SELECT 'UNKNOWN'
            """;

    private static final String TOTAL_COUNT_QUERY = """
            WITH source AS (
""" + SOURCE_QUERY + """
            )
            SELECT COUNT(*) FROM source
            """;

    private static final String MERGE_QUERY = """
            MERGE INTO MAXIMO.DPAMADPTVARIANT AS target
            USING (
                VALUES (?, ?)
            ) AS source (
                ADAPTERNAME,
                ADAPTERVARIANT
            )
            ON target.ADAPTERVARIANT = source.ADAPTERVARIANT
            WHEN NOT MATCHED THEN
                INSERT (
                    DPAMADPTVARIANTID,
                    ADAPTERNAME,
                    ADAPTERVARIANT
                )
                VALUES (
                    NEXT VALUE FOR MAXIMO.DPAMADPTVARIANTSEQ,
                    source.ADAPTERNAME,
                    source.ADAPTERVARIANT
                )
            """;

}
