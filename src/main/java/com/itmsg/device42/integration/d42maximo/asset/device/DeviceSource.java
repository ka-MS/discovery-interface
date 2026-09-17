package com.itmsg.device42.integration.d42maximo.asset.device;

import java.time.LocalDateTime;

public record DeviceSource(
        Integer devicePk,
        String name,
        String type,
        String notes,
        String serialNo,
        String assetNo,
        String uuid,
        Boolean networkDevice,
        String physicalSubtype,
        String hardwareName,
        String vendorName,
        Boolean inService,
        LocalDateTime lastDiscovered
) {
}
