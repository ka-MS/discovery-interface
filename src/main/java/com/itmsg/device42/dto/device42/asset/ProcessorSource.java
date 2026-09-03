package com.itmsg.device42.dto.device42.asset;

import java.math.BigDecimal;

public record ProcessorSource(
        Long partPk,
        Long deviceFk,
        String slot,
        String description,
        String modelName,
        Integer cores,
        BigDecimal speed,
        String speedUnit,
        String vendorName
) {
}
