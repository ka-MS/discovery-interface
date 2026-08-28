package com.itmsg.device42.integration.conversion;

import com.itmsg.device42.dto.device42.Device42OsNameSource;
import com.itmsg.device42.dto.maximo.DpamOsUpsert;
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
@Order(3)
public class DpamOsIntegrate implements ConversionIntegrationTask {

    private static final Logger log = LoggerFactory.getLogger(DpamOsIntegrate.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final Device42ConnectionFactory connectionFactory;
    private final JdbcTemplate maximoJdbcTemplate;

    public DpamOsIntegrate(
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
            log.info("배치할 운영체제 변환 대상 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        for (long offset = 0; offset < totalCount; offset += batchSize) {
            int limit = (int) Math.min(batchSize, totalCount - offset);

            List<Device42OsNameSource> data = getData(offset, limit);

            List<DpamOsUpsert> mappedData = mapData(data);

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
            throw new IllegalStateException("운영체제 변환 대상 건수 조회에 실패했습니다.", e);
        }
    }

    public List<Device42OsNameSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            List<Device42OsNameSource> rows = new ArrayList<>(limit);

            while (resultSet.next()) {
                rows.add(new Device42OsNameSource(resultSet.getString("name")));
            }

            return rows;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "운영체제 변환 대상 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private List<DpamOsUpsert> mapData(List<Device42OsNameSource> data) {
        List<DpamOsUpsert> mappedData = new ArrayList<>(data.size());

        for (Device42OsNameSource source : data) {
            String name = trimToNull(source.osName());
            if (name != null) {
                mappedData.add(new DpamOsUpsert(name));
            }
        }

        return mappedData;
    }

    public void putData(List<DpamOsUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_QUERY,
                (PreparedStatement statement) -> {
                    for (DpamOsUpsert row : data) {
                        try {
                            statement.setString(1, row.osName());
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error("운영체제 변환 대상 MERGE에 실패했습니다. name={}", row.osName(), e);
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
            MERGE INTO MAXIMO.DPAMOS AS target
            USING (
                VALUES (?)
            ) AS source (
                OSNAME
            )
            ON target.OSNAME = source.OSNAME
            WHEN NOT MATCHED THEN
                INSERT (
                    OSID,
                    OSNAME,
                    VALIDATED
                )
                VALUES (
                    NEXT VALUE FOR MAXIMO.DPAMOSSEQ,
                    source.OSNAME,
                    0
                )
            """;

}
