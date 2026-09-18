package com.itmsg.device42.source.device42.asset.computer;

import com.itmsg.device42.source.device42.selection.DeviceSelection;

import com.itmsg.device42.source.device42.DoqlClient;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ComputerQuery {

    private final DoqlClient doql;
    private final DeviceSelection selection;

    public ComputerQuery(DeviceSelection selection, DoqlClient doql) {
        this.doql = doql;
        this.selection = selection;
    }

    public long getTotalCount() {
        try {
            return doql.preparedQuery(sql(DEVICE_TOTAL_COUNT_QUERY), resultSet -> {

                return resultSet.next() ? resultSet.getLong(1) : 0L;
            });
        } catch (SQLException e) {
            throw new IllegalStateException("DPA Computer 대상 장비 건수 조회에 실패했습니다.", e);
        }
    }

    public List<ComputerHardwareSource> getData(long offset, int limit) {
        String query = sql(DEVICE_QUERY) + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try {
            return doql.query(query, resultSet -> {

                List<ComputerHardwareSource> computers = new ArrayList<>(limit);
                while (resultSet.next()) {
                    computers.add(new ComputerHardwareSource(
                            resultSet.getInt("device_pk"),
                            resultSet.getString("bios_name"),
                            resultSet.getString("bios_version"),
                            resultSet.getString("bios_release_date"),
                            resultSet.getBigDecimal("ram"),
                            resultSet.getString("ram_size_type"),
                            getNullableInteger(resultSet, "total_cpus"),
                            getNullableInteger(resultSet, "core_per_cpu")
                    ));
                }
                return computers;
            });
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "DPA Computer 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    /**
     * 숫자형 조회값을 nullable Integer로 변환한다.
     * 소수이거나 Integer 범위를 벗어난 값은 데이터 오류로 처리한다.
     */
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

    private String sql(String template) {
        return template
                .replace("{{DEVICE_FILTER}}", selection.sql())
                .replace("{{COMPUTER_FILTER}}", selection.sql());
    }

    private static final String DEVICE_FILTER = "{{DEVICE_FILTER}}";

    private static final String DEVICE_TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            FROM view_device_v2 d
            WHERE
            """ + DEVICE_FILTER;

    private static final String DEVICE_QUERY = """
            SELECT
                d.device_pk,
                v.name AS bios_name,
                d.bios_version,
                d.bios_release_date,
                d.ram,
                d.ram_size_type,
                d.total_cpus,
                d.core_per_cpu
            FROM view_device_v2 d
            LEFT JOIN view_vendor_v1 v
                ON v.vendor_pk = d.bios_vendor_fk
            WHERE
            """ + DEVICE_FILTER + """
            ORDER BY d.device_pk
            """;
}
