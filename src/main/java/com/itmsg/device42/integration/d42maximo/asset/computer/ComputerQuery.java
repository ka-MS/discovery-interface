package com.itmsg.device42.integration.d42maximo.asset.computer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.itmsg.device42.config.Device42ConnectionFactory;
import com.itmsg.device42.dto.device42.asset.ComputerHardwareSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ComputerQuery {

    public ComputerQuery(Device42ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    private final Device42ConnectionFactory connectionFactory;

    public long getTotalCount() {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(DEVICE_TOTAL_COUNT_QUERY);
             ResultSet resultSet = statement.executeQuery()) {

            return resultSet.next() ? resultSet.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new IllegalStateException("DPA Computer 대상 장비 건수 조회에 실패했습니다.", e);
        }
    }

    public List<ComputerHardwareSource> getData(long offset, int limit) {
        String query = DEVICE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

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

    private static final String DEVICE_FILTER = """
            d.type IN ('virtual', 'physical')
            AND (
                d.virtualsubtype_id IS NULL
                OR d.virtualsubtype_id <> 15
            )
            AND (d.network_device = false OR d.network_device IS NULL)
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype NOT IN ('Network Printer', 'PDU')
            )
            """;

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
