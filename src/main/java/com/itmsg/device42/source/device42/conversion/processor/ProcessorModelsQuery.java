package com.itmsg.device42.source.device42.conversion.processor;

import com.itmsg.device42.source.device42.selection.DeviceSelection;

import com.itmsg.device42.source.device42.DoqlClient;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProcessorModelsQuery {

    private final DoqlClient doql;
    private final DeviceSelection selection;

    public ProcessorModelsQuery(DeviceSelection selection, DoqlClient doql) {
        this.doql = doql;
        this.selection = selection;
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
            throw new IllegalStateException("프로세서 변환 대상 건수 조회에 실패했습니다.", e);
        }
    }

    public List<ProcessorModelSource> getData(long offset, int limit) {
        String query = sql(SOURCE_QUERY) + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try {
            return doql.query(query, resultSet -> {

                List<ProcessorModelSource> rows = new ArrayList<>(limit);

                while (resultSet.next()) {
                    rows.add(new ProcessorModelSource(resultSet.getString("name")));
                }

                return rows;
            });
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "프로세서 변환 대상 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private String sql(String template) {
        return template
                .replace("{{DEVICE_FILTER}}", selection.sql())
                .replace("{{COMPUTER_FILTER}}", selection.sql());
    }

    private static final String SOURCE_QUERY = """
            SELECT DISTINCT pm.name
            FROM view_part_v1 p
            JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
            JOIN view_device_v2 d ON d.device_pk = p.device_fk
            WHERE pm.type_name = 'CPU'
              AND
            {{DEVICE_FILTER}}
              AND pm.name IS NOT NULL
              AND pm.name <> ''
            """;

    private static final String TOTAL_COUNT_QUERY = """
            WITH source AS (
""" + SOURCE_QUERY + """
            )
            SELECT COUNT(*) FROM source
            """;
}
