package com.itmsg.device42.pipeline.d42maximo.asset.device;

import com.itmsg.device42.source.device42.asset.device.DeviceSource;

import com.itmsg.device42.target.maximo.asset.DeployedAssetUpsert;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DeployedAssetMapper {

    public List<DeployedAssetUpsert> mapData(List<DeviceSource> data) {
        LocalDateTime applyDateTime = LocalDateTime.now();
        List<DeployedAssetUpsert> mappedData = new ArrayList<>(data.size());

        for (DeviceSource source : data) {
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

            mappedData.add(new DeployedAssetUpsert(
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
                    assetClass,
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
}
