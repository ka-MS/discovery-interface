package com.itmsg.device42.integration.asset;

import com.itmsg.device42.dto.device42.Device42DpaTcpIpSource;
import com.itmsg.device42.dto.maximo.DpaTcpIpUpsert;
import com.itmsg.device42.config.Device42ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(9)
public class DpaTcpIpIntegrate implements AssetIntegrationTask {

    private static final Logger log = LoggerFactory.getLogger(DpaTcpIpIntegrate.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final Device42ConnectionFactory connectionFactory;
    private final JdbcTemplate maximoJdbcTemplate;

    public DpaTcpIpIntegrate(
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
            log.info("배치할 DPA TCP/IP 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        for (long offset = 0; offset < totalCount; offset += batchSize) {
            int limit = (int) Math.min(batchSize, totalCount - offset);

            List<Device42DpaTcpIpSource> data = getData(offset, limit);

            List<DpaTcpIpUpsert> mappedData = mapData(data);

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
            throw new IllegalStateException("DPA TCP/IP 대상 IP 건수 조회에 실패했습니다.", e);
        }
    }

    public List<Device42DpaTcpIpSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            List<Device42DpaTcpIpSource> rows = new ArrayList<>(limit);

            while (resultSet.next()) {
                rows.add(new Device42DpaTcpIpSource(
                        resultSet.getLong("ipaddress_pk"),
                        resultSet.getLong("device_fk"),
                        resultSet.getString("device_name"),
                        resultSet.getString("ip_address"),
                        resultSet.getString("gateway"),
                        getNullableInteger(resultSet, "mask_bits")
                ));
            }

            return rows;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "DPA TCP/IP 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private List<DpaTcpIpUpsert> mapData(List<Device42DpaTcpIpSource> data) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<DpaTcpIpUpsert> mappedData = new ArrayList<>(data.size());

        for (Device42DpaTcpIpSource source : data) {
            mappedData.add(new DpaTcpIpUpsert(
                    source.ipAddressPk(),
                    source.deviceFk(),
                    source.gateway(),
                    source.deviceName(),
                    source.ipAddress(),
                    toIpv4Netmask(source.maskBits()),
                    applyDateTime,
                    applyDateTime
            ));
        }

        return mappedData;
    }

    public void putData(List<DpaTcpIpUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_DPA_TCP_IP_QUERY,
                (PreparedStatement statement) -> {
                    for (DpaTcpIpUpsert tcpIp : data) {
                        try {
                            statement.setLong(1, tcpIp.tcpIpId());
                            statement.setString(2, tcpIp.gateway());
                            statement.setString(3, tcpIp.host());
                            statement.setLong(4, tcpIp.nodeId());
                            statement.setString(5, tcpIp.tcpIpAddress());
                            statement.setString(6, tcpIp.tcpIpNetmask());
                            statement.setTimestamp(7, toTimestamp(tcpIp.createDate()));
                            statement.setTimestamp(8, toTimestamp(tcpIp.changeDate()));
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error(
                                    "DPA TCP/IP MERGE에 실패했습니다. tcpIpId={}, nodeId={}",
                                    tcpIp.tcpIpId(),
                                    tcpIp.nodeId(),
                                    e
                            );
                        }
                    }
                    return null;
                }
        );
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

    static String toIpv4Netmask(Integer maskBits) {
        if (maskBits == null || maskBits == 0) {
            return null;
        }
        if (maskBits < 0 || maskBits > 32) {
            throw new IllegalArgumentException("IPv4 mask_bits 범위를 벗어났습니다: " + maskBits);
        }

        long mask = 0xFFFF_FFFFL << (32 - maskBits);
        return "%d.%d.%d.%d".formatted(
                (mask >>> 24) & 0xFF,
                (mask >>> 16) & 0xFF,
                (mask >>> 8) & 0xFF,
                mask & 0xFF
        );
    }

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
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
            FROM view_ipaddress_v2 i
            JOIN view_device_v2 d ON d.device_pk = ANY(i.device_fks)
            LEFT JOIN view_subnet_v1 b ON b.subnet_pk = i.subnet_fk
            WHERE
            """ + DEVICE_FILTER;

    private static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(DISTINCT i.ipaddress_pk)
            """ + SOURCE_FROM_AND_FILTER;

    private static final String SOURCE_QUERY = """
            SELECT DISTINCT ON (i.ipaddress_pk)
                i.ipaddress_pk,
                d.device_pk AS device_fk,
                d.name AS device_name,
                HOST(i.ip_address) AS ip_address,
                b.gateway,
                b.mask_bits
            """ + SOURCE_FROM_AND_FILTER + """
            ORDER BY i.ipaddress_pk, d.device_pk
            """;

    private static final String MERGE_DPA_TCP_IP_QUERY = """
            MERGE INTO MAXIMO.DPATCPIP AS target
            USING (
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ) AS source (
                TCPIPID,
                GATEWAY,
                HOST,
                NODEID,
                TCPIPADDRESS,
                TCPIPNETMASK,
                CREATEDATE,
                CHANGEDATE
            )
            ON target.TCPIPID = source.TCPIPID
            WHEN MATCHED THEN
                UPDATE SET
                    GATEWAY = source.GATEWAY,
                    HOST = source.HOST,
                    NODEID = source.NODEID,
                    TCPIPADDRESS = source.TCPIPADDRESS,
                    TCPIPNETMASK = source.TCPIPNETMASK,
                    CHANGEDATE = source.CHANGEDATE
            WHEN NOT MATCHED THEN
                INSERT (
                    TCPIPID,
                    GATEWAY,
                    HOST,
                    NODEID,
                    TCPIPADDRESS,
                    TCPIPNETMASK,
                    CREATEDATE,
                    CHANGEDATE
                )
                VALUES (
                    source.TCPIPID,
                    source.GATEWAY,
                    source.HOST,
                    source.NODEID,
                    source.TCPIPADDRESS,
                    source.TCPIPNETMASK,
                    source.CREATEDATE,
                    source.CHANGEDATE
                )
            """;
}
