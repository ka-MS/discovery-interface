package com.itmsg.device42.integration.asset;

import com.itmsg.device42.dto.device42.asset.NetworkInterfaceSource;
import com.itmsg.device42.dto.maximo.asset.DpaNetAdapterUpsert;
import com.itmsg.device42.config.Device42ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@Order(7)
public class DpaNetAdapterIntegrate implements AssetIntegrationTask {

    private static final Logger log = LoggerFactory.getLogger(DpaNetAdapterIntegrate.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final String UNKNOWN = "UNKNOWN";
    private static final String ADAPTER_TYPE = "Network Adapter";
    private static final int PORT_MAX_LENGTH = 16;

    private final Device42ConnectionFactory connectionFactory;
    private final JdbcTemplate maximoJdbcTemplate;

    public DpaNetAdapterIntegrate(
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
            log.info("배치할 DPA 네트워크 어댑터 데이터가 없습니다. totalcount={}", totalCount);
            return;
        }

        for (long offset = 0; offset < totalCount; offset += batchSize) {
            int limit = (int) Math.min(batchSize, totalCount - offset);

            List<NetworkInterfaceSource> data = getData(offset, limit);

            List<DpaNetAdapterUpsert> mappedData = mapData(data);

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
            throw new IllegalStateException("DPA 네트워크 어댑터 대상 포트 건수 조회에 실패했습니다.", e);
        }
    }

    public List<NetworkInterfaceSource> getData(long offset, int limit) {
        String query = SOURCE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            List<NetworkInterfaceSource> rows = new ArrayList<>(limit);

            while (resultSet.next()) {
                rows.add(new NetworkInterfaceSource(
                        resultSet.getLong("netport_pk"),
                        resultSet.getLong("device_fk"),
                        resultSet.getString("port"),
                        resultSet.getString("description"),
                        resultSet.getString("hwaddress"),
                        resultSet.getString("hwaddress2"),
                        resultSet.getString("port_speed"),
                        resultSet.getString("global_type"),
                        resultSet.getString("vendor_name")
                ));
            }

            return rows;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "DPA 네트워크 어댑터 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private List<DpaNetAdapterUpsert> mapData(List<NetworkInterfaceSource> data) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<DpaNetAdapterUpsert> mappedData = new ArrayList<>(data.size());

        for (NetworkInterfaceSource source : data) {
            mappedData.add(new DpaNetAdapterUpsert(
                    source.netportPk(),
                    source.deviceFk(),
                    ADAPTER_TYPE,
                    parseBandwidth(source.portSpeed()),
                    parseBandwidthUnit(source.portSpeed()),
                    trimToNull(source.description()),
                    UNKNOWN,
                    defaultUnknown(source.vendorName()),
                    upperToNull(source.hwaddress()),
                    upperToNull(source.hwaddress2()),
                    truncate(source.port(), PORT_MAX_LENGTH),
                    trimToNull(source.globalType()),
                    applyDateTime,
                    applyDateTime
            ));
        }

        return mappedData;
    }

    public void putData(List<DpaNetAdapterUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_DPA_NET_ADAPTER_QUERY,
                (PreparedStatement statement) -> {
                    for (DpaNetAdapterUpsert adapter : data) {
                        try {
                            statement.setLong(1, adapter.adapterId());
                            statement.setString(2, adapter.adapterType());
                            statement.setBigDecimal(3, adapter.bandwidth());
                            statement.setString(4, adapter.bandwidthUnit());
                            statement.setString(5, adapter.description());
                            statement.setString(6, adapter.makeModel());
                            statement.setString(7, adapter.manufacturer());
                            statement.setString(8, adapter.netMacAddr1());
                            statement.setString(9, adapter.netMacAddr2());
                            statement.setLong(10, adapter.nodeId());
                            statement.setString(11, adapter.port());
                            statement.setString(12, adapter.protocol());
                            statement.setTimestamp(13, toTimestamp(adapter.createDate()));
                            statement.setTimestamp(14, toTimestamp(adapter.changeDate()));
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error(
                                    "DPA 네트워크 어댑터 MERGE에 실패했습니다. adapterId={}, nodeId={}",
                                    adapter.adapterId(),
                                    adapter.nodeId(),
                                    e
                            );
                        }
                    }
                    return null;
                }
        );
    }

    static BigDecimal parseBandwidth(String portSpeed) {
        String normalized = trimToNull(portSpeed);
        if (normalized == null) {
            return null;
        }
        int separator = normalized.indexOf(' ');
        String number = separator < 0 ? normalized : normalized.substring(0, separator);
        try {
            return new BigDecimal(number).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String parseBandwidthUnit(String portSpeed) {
        String normalized = trimToNull(portSpeed);
        if (normalized == null) {
            return null;
        }
        int separator = normalized.indexOf(' ');
        return separator < 0 ? null : trimToNull(normalized.substring(separator + 1));
    }

    static String upperToNull(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    static String truncate(String value, int maxLength) {
        String normalized = trimToNull(value);
        if (normalized == null || normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    static String defaultUnknown(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? UNKNOWN : normalized;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
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
            FROM view_netport_v1 n
            JOIN view_device_v2 d ON d.device_pk = n.device_fk
            LEFT JOIN view_vendor_v1 v ON v.vendor_pk = n.vendor_fk
            WHERE
            """ + DEVICE_FILTER;

    private static final String TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            """ + SOURCE_FROM_AND_FILTER;

    private static final String SOURCE_QUERY = """
            SELECT
                n.netport_pk,
                n.device_fk,
                n.port,
                n.description,
                n.hwaddress,
                n.hwaddress2,
                n.port_speed,
                n.global_type,
                v.name AS vendor_name
            """ + SOURCE_FROM_AND_FILTER + """
            ORDER BY n.device_fk, n.netport_pk
            """;

    private static final String MERGE_DPA_NET_ADAPTER_QUERY = """
            MERGE INTO MAXIMO.DPANETADAPTER AS target
            USING (
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ) AS source (
                ADAPTERID,
                ADAPTERTYPE,
                BANDWIDTH,
                BANDWIDTHUNIT,
                DESCRIPTION,
                MAKEMODEL,
                MANUFACTURER,
                NETMACADDR1,
                NETMACADDR2,
                NODEID,
                PORT,
                PROTOCOL,
                CREATEDATE,
                CHANGEDATE
            )
            ON target.ADAPTERID = source.ADAPTERID
            WHEN MATCHED THEN
                UPDATE SET
                    ADAPTERTYPE = source.ADAPTERTYPE,
                    BANDWIDTH = source.BANDWIDTH,
                    BANDWIDTHUNIT = source.BANDWIDTHUNIT,
                    DESCRIPTION = source.DESCRIPTION,
                    MAKEMODEL = source.MAKEMODEL,
                    MANUFACTURER = source.MANUFACTURER,
                    NETMACADDR1 = source.NETMACADDR1,
                    NETMACADDR2 = source.NETMACADDR2,
                    NODEID = source.NODEID,
                    PORT = source.PORT,
                    PROTOCOL = source.PROTOCOL,
                    CHANGEDATE = source.CHANGEDATE
            WHEN NOT MATCHED THEN
                INSERT (
                    ADAPTERID,
                    ADAPTERTYPE,
                    BANDWIDTH,
                    BANDWIDTHUNIT,
                    DESCRIPTION,
                    MAKEMODEL,
                    MANUFACTURER,
                    NETMACADDR1,
                    NETMACADDR2,
                    NODEID,
                    PORT,
                    PROTOCOL,
                    CREATEDATE,
                    CHANGEDATE
                )
                VALUES (
                    source.ADAPTERID,
                    source.ADAPTERTYPE,
                    source.BANDWIDTH,
                    source.BANDWIDTHUNIT,
                    source.DESCRIPTION,
                    source.MAKEMODEL,
                    source.MANUFACTURER,
                    source.NETMACADDR1,
                    source.NETMACADDR2,
                    source.NODEID,
                    source.PORT,
                    source.PROTOCOL,
                    source.CREATEDATE,
                    source.CHANGEDATE
                )
            """;
}
