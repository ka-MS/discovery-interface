package com.itmsg.device42.source.device42.asset.tcpip;

import com.itmsg.device42.source.device42.selection.DeviceSelection;

import com.itmsg.device42.source.device42.DoqlClient;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TcpIpQuery {

    private final DoqlClient doql;
    private final DeviceSelection selection;

    public TcpIpQuery(DeviceSelection selection, DoqlClient doql) {
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
            throw new IllegalStateException("DPA TCP/IP 대상 IP 건수 조회에 실패했습니다.", e);
        }
    }

    public List<IpAddressSource> getData(long offset, int limit) {
        String query = sql(SOURCE_QUERY) + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try {
            return doql.query(query, resultSet -> {

                List<IpAddressSource> rows = new ArrayList<>(limit);

                while (resultSet.next()) {
                    rows.add(new IpAddressSource(
                            resultSet.getLong("ipaddress_pk"),
                            resultSet.getLong("device_fk"),
                            resultSet.getString("device_name"),
                            resultSet.getString("ip_address"),
                            resultSet.getString("ip_hybrid"),
                            resultSet.getString("label"),
                            getNullableLong(resultSet, "subnet_fk"),
                            getNullableLong(resultSet, "type_id"),
                            resultSet.getString("type"),
                            getNullableBoolean(resultSet, "available"),
                            getNullableBoolean(resultSet, "is_public"),
                            getNullableLong(resultSet, "resource_fk"),
                            resultSet.getString("notes"),
                            resultSet.getString("first_added"),
                            resultSet.getString("last_edited"),
                            resultSet.getString("tags"),
                            getNullableLong(resultSet, "netport_fk"),
                            resultSet.getString("details"),
                            resultSet.getString("last_changed"),
                            resultSet.getString("last_discovered"),
                            getNullableBoolean(resultSet, "is_shared"),
                            getNullableLong(resultSet, "cloudinfrastructure_fk"),
                            resultSet.getString("gateway"),
                            getNullableInteger(resultSet, "mask_bits")
                    ));
                }

                return rows;
            });
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "DPA TCP/IP 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
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

    private static Long getNullableLong(ResultSet resultSet, String column) throws SQLException {
        BigDecimal value = resultSet.getBigDecimal(column);
        if (value == null) {
            return null;
        }
        try {
            return value.longValueExact();
        } catch (ArithmeticException e) {
            throw new SQLException(column + " 값을 long으로 변환할 수 없습니다: " + value, e);
        }
    }

    private static Boolean getNullableBoolean(ResultSet resultSet, String column) throws SQLException {
        boolean value = resultSet.getBoolean(column);
        return resultSet.wasNull() ? null : value;
    }

    private String sql(String template) {
        return template
                .replace("{{DEVICE_FILTER}}", selection.sql())
                .replace("{{COMPUTER_FILTER}}", selection.sql());
    }

    private static final String DEVICE_FILTER = "{{DEVICE_FILTER}}";

    private static final String SOURCE_FROM_AND_FILTER = """
            FROM view_ipaddress_v2 i
            JOIN view_device_v2 d ON d.device_pk = ANY(i.device_fks)
            LEFT JOIN view_subnet_v1 b ON b.subnet_pk = i.subnet_fk
            WHERE
            """ + DEVICE_FILTER;

    private static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            """ + SOURCE_FROM_AND_FILTER;

    private static final String SOURCE_QUERY = """
            SELECT
                i.ipaddress_pk,
                d.device_pk AS device_fk,
                d.name AS device_name,
                HOST(i.ip_address) AS ip_address,
                i.ip_hybrid,
                i.label,
                i.subnet_fk,
                i.type_id,
                i.type,
                i.available,
                i.is_public,
                i.resource_fk,
                i.notes,
                i.first_added,
                i.last_edited,
                i.tags,
                i.netport_fk,
                i.details,
                i.last_changed,
                i.last_discovered,
                i.is_shared,
                i.cloudinfrastructure_fk,
                b.gateway,
                b.mask_bits
            """ + SOURCE_FROM_AND_FILTER + """
            ORDER BY i.ipaddress_pk, d.device_pk
            """;
}
