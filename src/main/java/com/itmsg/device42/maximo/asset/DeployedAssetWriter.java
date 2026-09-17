package com.itmsg.device42.maximo.asset;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

import com.itmsg.device42.dto.maximo.asset.DeployedAssetUpsert;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DeployedAssetWriter {

    public DeployedAssetWriter(@Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate) {
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    private static final Logger log = LoggerFactory.getLogger(DeployedAssetWriter.class);

    private final JdbcTemplate maximoJdbcTemplate;

    public void write(List<DeployedAssetUpsert> datas) {
        maximoJdbcTemplate.execute(
                MERGE_DEPLOYED_ASSET_QUERY,
                (PreparedStatement statement) -> {
                    for (DeployedAssetUpsert asset : datas) {
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
                            statement.setString(14, asset.systemRole());
                            statement.setString(15, asset.assetClass());
                            statement.setTimestamp(16, toTimestamp(asset.createDate()));
                            statement.setTimestamp(17, toTimestamp(asset.changeDate()));
                            statement.setString(18, asset.tloamStatus());
                            statement.setString(19, asset.tloamNrsManufacturer());
                            statement.setString(20, asset.tloamNrsUuid());
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

    private static final String MERGE_DEPLOYED_ASSET_QUERY = """
            MERGE INTO MAXIMO.DEPLOYEDASSET AS target
            USING (
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                SYSTEMROLE,
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
                    SYSTEMROLE = source.SYSTEMROLE,
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
                    SYSTEMROLE,
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
                    source.SYSTEMROLE,
                    source.ASSETCLASS,
                    source.CREATEDATE,
                    source.CHANGEDATE,
                    source.TLOAMSTATUS,
                    source.TLOAMNRSMANUFACTURER,
                    source.TLOAMNRSUUID
                )
            """;
}
