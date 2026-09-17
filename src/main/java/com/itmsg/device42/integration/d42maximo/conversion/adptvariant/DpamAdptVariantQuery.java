package com.itmsg.device42.integration.d42maximo.conversion.adptvariant;

import com.itmsg.device42.config.Device42ConnectionFactory;
import com.itmsg.device42.dto.device42.conversion.AdapterModelSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DpamAdptVariantQuery {

    public DpamAdptVariantQuery(Device42ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    private final Device42ConnectionFactory connectionFactory;

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
}
