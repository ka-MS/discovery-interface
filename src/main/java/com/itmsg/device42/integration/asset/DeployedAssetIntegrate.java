package com.itmsg.device42.integration.asset;

import com.itmsg.device42.dto.device42.Device42DeployedAssetSource;
import com.itmsg.device42.dto.maximo.DeployedAsset;
import com.itmsg.device42.integration.config.Device42ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(1)
public class DeployedAssetIntegrate implements AssetIntegrationTask {

    private static final Logger log = LoggerFactory.getLogger(DeployedAssetIntegrate.class);

    private final Device42ConnectionFactory connectionFactory;
    private final JdbcTemplate maximoJdbcTemplate;

    private static final int DEFAULT_BATCH_SIZE = 1000;

    public DeployedAssetIntegrate(
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

        if (totalCount <= 0 || batchSize <= 0) {
            return;
        }

        for (long offset = 0; offset < totalCount; offset += batchSize) {
            int limit = (int) Math.min((long) batchSize, totalCount - offset);
            List<Device42DeployedAssetSource> data = getData(offset, limit);

            if (data == null || data.isEmpty()) {
                continue;
            }

            List<DeployedAsset> mappedData = mapData(data);

            if (mappedData.isEmpty()) {
                continue;
            }

            putData(mappedData);
        }
    }

    public long getTotalCount() {
        try (Connection connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(DEVICE_TOTAL_COUNT_QUERY);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return resultSet.getLong(1);
            }

            return 0L;
        } catch (SQLException e) {
            throw new IllegalStateException("장비 건수 조회에 실패했습니다.", e);
        }
    }

    public List<Device42DeployedAssetSource> getData(long offset, int limit) {

        String query = DEVICE_QUERY + "LIMIT %d OFFSET %d".formatted(limit, offset);

        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            List<Device42DeployedAssetSource> devices = new ArrayList<>(limit);

            while (resultSet.next()) {
                devices.add(new Device42DeployedAssetSource(
                        resultSet.getInt("device_pk"),
                        resultSet.getString("name"),
                        resultSet.getString("type"),
                        resultSet.getString("notes"),
                        resultSet.getString("serial_no"),
                        resultSet.getString("asset_no"),
                        resultSet.getString("uuid"),
                        resultSet.getBoolean("network_device"),
                        resultSet.getString("physicalsubtype"),
                        resultSet.getString("hardware_name"),
                        resultSet.getString("vendor_name"),
                        resultSet.getBoolean("in_service"),
                        getNullableLocalDateTime(resultSet, "last_discovered"),
                        resultSet.getBigDecimal("ram"),
                        resultSet.getString("ram_size_type"),
                        resultSet.getInt("total_cpus"),
                        resultSet.getInt("core_per_cpu"),
                        resultSet.getString("bios_version"),
                        getNullableLocalDate(resultSet, "bios_release_date")
                ));
            }

            return devices;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "데이터 조회에 실패했습니다. offset=" + offset + ", limit=" + limit,
                    e
            );
        }
    }

    private List<DeployedAsset> mapData(List<Device42DeployedAssetSource> data) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<DeployedAsset> mappedData = new ArrayList<>(data.size());

        for (Device42DeployedAssetSource source : data) {
            String assetClass;

            if (Boolean.TRUE.equals(source.networkDevice())) {
                assetClass = "NETDEVICE";
            } else if ("Network Printer".equals(source.physicalSubtype())) {
                assetClass = "NETPRINTER";
            } else if (source.type() != null
                    && !source.type().isBlank()
                    && !"unknown".equalsIgnoreCase(source.type().trim())) {
                assetClass = "COMPUTER";
            } else {
                assetClass = "COMPUTER";
            }

            String manufacturer = source.vendorName() == null || source.vendorName().isBlank()
                    ? "UNKNOWN"
                    : source.vendorName();
            String nodeName = source.name() == null || source.name().isBlank()
                    ? "UNKNOWN"
                    : source.name();

            mappedData.add(new DeployedAsset(
                    (long) source.devicePk(),
                    nodeName,
                    "UNKNOWN",
                    source.serialNo(),
                    source.assetNo(),
                    source.hardwareName(),
                    manufacturer,
                    source.notes(),
                    source.lastDiscovered(),
                    "Device42",
                    0,
                    String.valueOf(source.devicePk()),
                    null,
                    assetClass,
                    null,
                    null,
                    applyDateTime,
                    applyDateTime,
                    null,
                    null,
                    null,
                    Boolean.TRUE.equals(source.inService()) ? "ACTIVE" : "INACTIVE",
                    null,
                    null,
                    null,
                    source.vendorName(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    source.uuid(),
                    null,
                    null,
                    "Device42",
                    null
            ));
        }

        return mappedData;
    }

    public void putData(List<DeployedAsset> datas) {
        maximoJdbcTemplate.execute(
                MERGE_DEPLOYED_ASSET_QUERY,
                (PreparedStatement statement) -> {
                    for (DeployedAsset asset : datas) {
                        try {
                            statement.setLong(1, asset.nodeId());
                            statement.setString(2, asset.sourceId());
                            statement.setString(3, asset.importSource());
                            statement.setString(4, asset.nodeName());
                            statement.setString(5, asset.domainName());
                            statement.setString(6, asset.serialNumber());
                            statement.setString(7, asset.assetTag());
                            statement.setString(8, asset.makeModel());
                            statement.setString(9, asset.manufacturer());
                            statement.setString(10, asset.description());
                            statement.setTimestamp(11, toTimestamp(asset.hwLastScanDate()));
                            statement.setString(12, asset.hwDetectionTool());
                            statement.setObject(13, asset.supportsSnmp(), Types.INTEGER);
                            statement.setString(14, asset.assetClass());
                            statement.setTimestamp(15, toTimestamp(asset.createDate()));
                            statement.setTimestamp(16, toTimestamp(asset.changeDate()));
                            statement.setString(17, asset.tloamStatus());
                            statement.setString(18, asset.tloamNrsManufacturer());
                            statement.setString(19, asset.tloamNrsUuid());
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            log.error(
                                    "DeployedAsset MERGE에 실패했습니다. sourceId={}",
                                    asset.sourceId(),
                                    e
                            );
                        }
                    }

                    return null;
                }
        );
    }

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private static LocalDateTime getNullableLocalDateTime(ResultSet resultSet, String column) throws SQLException {
        Timestamp value = resultSet.getTimestamp(column);
            return value == null ? null : value.toLocalDateTime();
    }

    private static LocalDate getNullableLocalDate(ResultSet resultSet, String column) throws SQLException {
        Date value = resultSet.getDate(column);
        return value == null ? null : value.toLocalDate();
    }

    private static final String DEVICE_FILTER = """
            d.type IN ('virtual', 'physical')
            AND (
                d.virtualsubtype_id IS NULL
                OR d.virtualsubtype_id <> 15
            )
            AND (
                d.physicalsubtype IS NULL
                OR d.physicalsubtype <> 'PDU'
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
                d.name,
                d.type,
                d.notes,
                d.serial_no,
                d.asset_no,
                d.uuid,
                d.network_device,
                d.physicalsubtype,
                h.name AS hardware_name,
                v.name AS vendor_name,
                d.in_service,
                d.last_discovered,
                d.ram,
                d.ram_size_type,
                d.total_cpus,
                d.core_per_cpu,
                d.bios_version,
                d.bios_release_date
            FROM view_device_v2 d
            LEFT JOIN view_hardware_v1 h
                ON d.hardware_fk = h.hardware_pk
            LEFT JOIN view_vendor_v1 v
                ON h.vendor_fk = v.vendor_pk
            WHERE
            """ + DEVICE_FILTER + """
            ORDER BY d.device_pk
            """;

    private static final String MERGE_DEPLOYED_ASSET_QUERY = """
            MERGE INTO MAXIMO.DEPLOYEDASSET AS target
            USING (
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ) AS source (
                NODEID,
                SOURCEID,
                IMPORTSOURCE,
                NODENAME,
                DOMAINNAME,
                SERIALNUMBER,
                ASSETTAG,
                MAKEMODEL,
                MANUFACTURER,
                DESCRIPTION,
                HWLASTSCANDATE,
                HWDETECTIONTOOL,
                SUPPORTSSNMP,
                ASSETCLASS,
                CREATEDATE,
                CHANGEDATE,
                TLOAMSTATUS,
                TLOAMNRSMANUFACTURER,
                TLOAMNRSUUID
            )
            ON target.NODEID = source.NODEID
            WHEN MATCHED THEN
                UPDATE SET
                    NODENAME = source.NODENAME,
                    DOMAINNAME = source.DOMAINNAME,
                    SERIALNUMBER = source.SERIALNUMBER,
                    ASSETTAG = source.ASSETTAG,
                    MAKEMODEL = source.MAKEMODEL,
                    MANUFACTURER = source.MANUFACTURER,
                    DESCRIPTION = source.DESCRIPTION,
                    HWLASTSCANDATE = source.HWLASTSCANDATE,
                    HWDETECTIONTOOL = source.HWDETECTIONTOOL,
                    SUPPORTSSNMP = source.SUPPORTSSNMP,
                    ASSETCLASS = source.ASSETCLASS,
                    CHANGEDATE = source.CHANGEDATE,
                    TLOAMSTATUS = source.TLOAMSTATUS,
                    TLOAMNRSMANUFACTURER = source.TLOAMNRSMANUFACTURER,
                    TLOAMNRSUUID = source.TLOAMNRSUUID
            WHEN NOT MATCHED THEN
                INSERT (
                    NODEID,
                    SOURCEID,
                    IMPORTSOURCE,
                    NODENAME,
                    DOMAINNAME,
                    SERIALNUMBER,
                    ASSETTAG,
                    MAKEMODEL,
                    MANUFACTURER,
                    DESCRIPTION,
                    HWLASTSCANDATE,
                    HWDETECTIONTOOL,
                    SUPPORTSSNMP,
                    ASSETCLASS,
                    CREATEDATE,
                    CHANGEDATE,
                    TLOAMSTATUS,
                    TLOAMNRSMANUFACTURER,
                    TLOAMNRSUUID
                )
                VALUES (
                    source.NODEID,
                    source.SOURCEID,
                    source.IMPORTSOURCE,
                    source.NODENAME,
                    source.DOMAINNAME,
                    source.SERIALNUMBER,
                    source.ASSETTAG,
                    source.MAKEMODEL,
                    source.MANUFACTURER,
                    source.DESCRIPTION,
                    source.HWLASTSCANDATE,
                    source.HWDETECTIONTOOL,
                    source.SUPPORTSSNMP,
                    source.ASSETCLASS,
                    source.CREATEDATE,
                    source.CHANGEDATE,
                    source.TLOAMSTATUS,
                    source.TLOAMNRSMANUFACTURER,
                    source.TLOAMNRSUUID
                )
            """;
}
