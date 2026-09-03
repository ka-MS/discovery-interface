package com.itmsg.device42.integration.asset;

import com.itmsg.device42.dto.device42.asset.NetworkPrinterSource;
import com.itmsg.device42.dto.maximo.DpaNetPrinterUpsert;
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

@Component
@Order(4)
public class DpaNetPrinterIntegrate implements AssetIntegrationTask {

    private static final Logger log = LoggerFactory.getLogger(DpaNetPrinterIntegrate.class);

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final Device42ConnectionFactory connectionFactory;
    private final JdbcTemplate maximoJdbcTemplate;

    public DpaNetPrinterIntegrate(
            Device42ConnectionFactory connectionFactory,
            @Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate
    ) {
        this.connectionFactory = connectionFactory;
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    @Override
    public void integrate() {
        long totalCount = getTotalCount();

        for (long offset = 0; offset < totalCount; offset += DEFAULT_BATCH_SIZE) {
            int limit = (int) Math.min(DEFAULT_BATCH_SIZE, totalCount - offset);
            List<NetworkPrinterSource> sourceData = getData(offset, limit);

            List<DpaNetPrinterUpsert> mappedData = mapData(sourceData);

            putData(mappedData);
        }
    }

    public long getTotalCount() {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(DEVICE_TOTAL_COUNT_QUERY);
             ResultSet resultSet = statement.executeQuery()) {

            return resultSet.next() ? resultSet.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new IllegalStateException("DPA NetPrinter 대상 장비 건수 조회에 실패했습니다.", e);
        }
    }

    public List<NetworkPrinterSource> getData(long offset, int limit) {
        String query = DEVICE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            List<NetworkPrinterSource> printers = new ArrayList<>(limit);
            while (resultSet.next()) {
                printers.add(new NetworkPrinterSource(
                        resultSet.getInt("device_pk"),
                        resultSet.getBigDecimal("ram"),
                        resultSet.getString("ram_size_type"),
                        resultSet.getString("hwaddress"),
                        resultSet.getString("ip_address"),
                        resultSet.getInt("tray_cnt")
                ));
            }
            return printers;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "DPA NetPrinter 원천 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private List<DpaNetPrinterUpsert> mapData(List<NetworkPrinterSource> sourceData) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<DpaNetPrinterUpsert> mappedData = new ArrayList<>(sourceData.size());

        for (NetworkPrinterSource source : sourceData) {
            mappedData.add(new DpaNetPrinterUpsert(
                    source.devicePk().longValue(),
                    roundCurrentRam(source.currentRam()),
                    source.macAddress(),
                    source.networkAddress(),
                    source.trayCount(),
                    source.ramUnit(),
                    applyDateTime,
                    applyDateTime
            ));
        }

        return mappedData;
    }

    public void putData(List<DpaNetPrinterUpsert> data) {
        maximoJdbcTemplate.execute(
                MERGE_DPA_NET_PRINTER_QUERY,
                (PreparedStatement statement) -> {
                    for (DpaNetPrinterUpsert printer : data) {
                        try {
                            statement.setLong(1, printer.nodeId());
                            statement.setBigDecimal(2, printer.currentRam());
                            statement.setString(3, printer.netMacAddress());
                            statement.setString(4, printer.networkAddress());
                            statement.setObject(5, printer.trayCount(), java.sql.Types.INTEGER);
                            statement.setString(6, printer.ramUnit());
                            statement.setTimestamp(7, toTimestamp(printer.createDate()));
                            statement.setTimestamp(8, toTimestamp(printer.changeDate()));
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error(
                                    "DPA NetPrinter MERGE에 실패했습니다. nodeId={}",
                                    printer.nodeId(),
                                    e
                            );
                        }
                    }
                    return null;
                }
        );
    }

    static BigDecimal roundCurrentRam(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private static final String DEVICE_TOTAL_COUNT_QUERY = """
            SELECT COUNT(*)
            FROM view_device_v2 d
            WHERE d.type IN ('virtual', 'physical')
              AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
              AND (d.network_device = false OR d.network_device IS NULL)
              AND d.physicalsubtype = 'Network Printer'
            """;

    private static final String DEVICE_QUERY = """
            SELECT
                d.device_pk,
                d.ram,
                d.ram_size_type,
                (SELECT UPPER(n.hwaddress)
                 FROM view_netport_v1 n
                 WHERE n.device_fk = d.device_pk AND n.hwaddress <> ''
                 LIMIT 1) AS hwaddress,
                (SELECT HOST(i.ip_address)
                 FROM view_ipaddress_v2 i
                 WHERE d.device_pk = ANY(i.device_fks)
                 LIMIT 1) AS ip_address,
                (SELECT COUNT(*)
                 FROM view_part_v1 p
                 JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
                 WHERE p.device_fk = d.device_pk AND pm.type_name = 'printer_input') AS tray_cnt
            FROM view_device_v2 d
            WHERE d.type IN ('virtual', 'physical')
              AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
              AND (d.network_device = false OR d.network_device IS NULL)
              AND d.physicalsubtype = 'Network Printer'
            ORDER BY d.device_pk
            """;

    private static final String MERGE_DPA_NET_PRINTER_QUERY = """
            MERGE INTO MAXIMO.DPANETPRINTER AS target
            USING (
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ) AS source (
                NODEID,
                CURRENTRAM,
                NETMACADDR,
                NETWORKADDRESS,
                NUMBEROFTRAYS,
                RAMUNIT,
                CREATEDATE,
                CHANGEDATE
            )
            ON target.NODEID = source.NODEID
            WHEN MATCHED THEN
                UPDATE SET
                    CURRENTRAM = source.CURRENTRAM,
                    NETMACADDR = source.NETMACADDR,
                    NETWORKADDRESS = source.NETWORKADDRESS,
                    NUMBEROFTRAYS = source.NUMBEROFTRAYS,
                    RAMUNIT = source.RAMUNIT,
                    CHANGEDATE = source.CHANGEDATE
            WHEN NOT MATCHED THEN
                INSERT (
                    NODEID,
                    CURRENTRAM,
                    NETMACADDR,
                    NETWORKADDRESS,
                    NUMBEROFTRAYS,
                    RAMUNIT,
                    CREATEDATE,
                    CHANGEDATE
                )
                VALUES (
                    source.NODEID,
                    source.CURRENTRAM,
                    source.NETMACADDR,
                    source.NETWORKADDRESS,
                    source.NUMBEROFTRAYS,
                    source.RAMUNIT,
                    source.CREATEDATE,
                    source.CHANGEDATE
                )
            """;
}
