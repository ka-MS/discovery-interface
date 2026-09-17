package com.itmsg.device42.integration.d42maximo.asset.cpu;

import com.itmsg.device42.device42.DoqlClient;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CpuQuery {

    private final DoqlClient doql;

    public CpuQuery(DoqlClient doql) {
        this.doql = doql;
    }

    public long getTotalCount() {
        try {
            return doql.preparedQuery(TOTAL_COUNT_QUERY, resultSet -> {

                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }

                return 0L;
            });
        } catch (SQLException e) {
            throw new IllegalStateException("DPA CPU 대상 파트 건수 조회에 실패했습니다.", e);
        }
    }

    public List<ProcessorSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try {
            return doql.query(query, resultSet -> {

                List<ProcessorSource> rows = new ArrayList<>(limit);

                while (resultSet.next()) {
                    rows.add(new ProcessorSource(
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
            });
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "DPA CPU 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
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
}
