package com.itmsg.device42.dto.device42;

import java.math.BigDecimal;

public record Device42DpaCpuSource(
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
