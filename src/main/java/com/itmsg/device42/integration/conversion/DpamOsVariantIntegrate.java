package com.itmsg.device42.integration.conversion;

import com.itmsg.device42.dto.device42.conversion.OperatingSystemNameSource;
import com.itmsg.device42.dto.maximo.DpamOsVariantUpsert;
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
@Order(4)
public class DpamOsVariantIntegrate implements ConversionIntegrationTask {

    private static final Logger log = LoggerFactory.getLogger(DpamOsVariantIntegrate.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final Device42ConnectionFactory connectionFactory;
    private final JdbcTemplate maximoJdbcTemplate;

    public DpamOsVariantIntegrate(
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
            log.info("배치할 운영체제 변환 변형 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        for (long offset = 0; offset < totalCount; offset += batchSize) {
            int limit = (int) Math.min(batchSize, totalCount - offset);

            List<OperatingSystemNameSource> data = getData(offset, limit);

            List<DpamOsVariantUpsert> mappedData = mapData(data);

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
            throw new IllegalStateException("운영체제 변환 변형 건수 조회에 실패했습니다.", e);
        }
    }

    public List<OperatingSystemNameSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            List<OperatingSystemNameSource> rows = new ArrayList<>(limit);

            while (resultSet.next()) {
                rows.add(new OperatingSystemNameSource(resultSet.getString("name")));
            }

            return rows;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "운영체제 변환 변형 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private List<DpamOsVariantUpsert> mapData(List<OperatingSystemNameSource> data) {
        List<DpamOsVariantUpsert> mappedData = new ArrayList<>(data.size());

        for (OperatingSystemNameSource source : data) {
            String variant = trimToNull(source.osName());
            if (variant != null) {
                mappedData.add(new DpamOsVariantUpsert(variant, variant));
            }
        }

        return mappedData;
    }

    public void putData(List<DpamOsVariantUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_QUERY,
                (PreparedStatement statement) -> {
                    for (DpamOsVariantUpsert row : data) {
                        try {
                            statement.setString(1, row.osName());
                            statement.setString(2, row.osVariant());
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error(
                                    "운영체제 변환 변형 MERGE에 실패했습니다. name={}, variant={}",
                                    row.osName(),
                                    row.osVariant(),
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
            SELECT DISTINCT o.os_name AS name
            FROM view_deviceos_v1 o
            JOIN view_device_v2 d ON d.device_pk = o.device_fk
            WHERE
            d.type IN ('virtual', 'physical')
            AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype NOT IN ('Network Printer', 'PDU')
            )
              AND o.os_name IS NOT NULL
              AND o.os_name <> ''
            """;

    private static final String TOTAL_COUNT_QUERY = """
            WITH source AS (
""" + SOURCE_QUERY + """
            )
            SELECT COUNT(*) FROM source
            """;

    private static final String MERGE_QUERY = """
            MERGE INTO MAXIMO.DPAMOSVARIANT AS target
            USING (
                VALUES (?, ?)
            ) AS source (
                OSNAME,
                OSVARIANT
            )
            ON target.OSVARIANT = source.OSVARIANT
            WHEN NOT MATCHED THEN
                INSERT (
                    DPAMOSVARIANTID,
                    OSNAME,
                    OSVARIANT
                )
                VALUES (
                    NEXT VALUE FOR MAXIMO.DPAMOSVARIANTSEQ,
                    source.OSNAME,
                    source.OSVARIANT
                )
            """;

}
