package com.itmsg.device42.source.device42.asset.disk;

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
