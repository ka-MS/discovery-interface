package com.itmsg.device42.integration.d42maximo.asset.disk;

import java.math.BigDecimal;

public record DiskSource(
        Long partPk,
        Long deviceFk,
        String serialNumber,
        String description,
        String modelName,
        BigDecimal totalSpace,
        String sizeUnit,
        String diskInterface,
        String mediaType,
        String vendorName
) {
}
