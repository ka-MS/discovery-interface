package com.itmsg.device42.dto.device42;

import java.math.BigDecimal;

public record Device42DpaDiskSource(
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
