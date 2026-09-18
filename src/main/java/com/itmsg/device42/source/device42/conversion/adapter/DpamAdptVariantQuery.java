package com.itmsg.device42.source.device42.conversion.adapter;

import com.itmsg.device42.source.device42.selection.DeviceSelection;

import com.itmsg.device42.source.device42.DoqlClient;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DpamAdptVariantQuery {

    private final DoqlClient doql;
    private final DeviceSelection selection;
    private final String supplementalName;

    public DpamAdptVariantQuery(DeviceSelection selection, String supplementalName, DoqlClient doql) {
        this.doql = doql;
        this.selection = selection;
        this.supplementalName = supplementalName;
    }

    public long getTotalCount() {
        try {
            return doql.preparedQuery(sql(TOTAL_COUNT_QUERY), resultSet -> {

                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }

                return 0L;
            });
        } catch (SQLException e) {
            throw new IllegalStateException("어댑터 변환 변형 건수 조회에 실패했습니다.", e);
        }
    }

    public List<AdapterModelSource> getData(long offset, int limit) {
        String query = sql(SOURCE_QUERY) + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try {
            return doql.query(query, resultSet -> {

                List<AdapterModelSource> rows = new ArrayList<>(limit);

                while (resultSet.next()) {
                    rows.add(new AdapterModelSource(resultSet.getString("name")));
                }

                return rows;
            });
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "어댑터 변환 변형 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private String sql(String template) {
        return template
                .replace("{{DEVICE_FILTER}}", selection.sql())
                .replace("{{COMPUTER_FILTER}}", selection.sql())
                .replace("{{SUPPLEMENTAL_NAME}}", DeviceSelection.literal(supplementalName));
    }

    private static final String SOURCE_QUERY = """
            WITH gpu AS (
                SELECT DISTINCT pm.name
                FROM view_part_v1 p
                JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
                JOIN view_device_v2 d ON d.device_pk = p.device_fk
                WHERE pm.type_name = 'GPU'
                  AND
            {{DEVICE_FILTER}}
                  AND pm.name IS NOT NULL
                  AND pm.name <> ''
            )
            SELECT name FROM gpu
            UNION
            SELECT {{SUPPLEMENTAL_NAME}}
            """;

    private static final String TOTAL_COUNT_QUERY = """
            WITH source AS (
""" + SOURCE_QUERY + """
            )
            SELECT COUNT(*) FROM source
            """;
}
