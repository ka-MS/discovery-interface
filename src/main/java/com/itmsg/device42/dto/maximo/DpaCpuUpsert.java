package com.itmsg.device42.dto.maximo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DpaCpuUpsert(
        Long sourceId,
        Long sourceParentId,
        Long nodeId,
        String cpuNum,
        BigDecimal currentSpeed,
        String description,
        Integer is64BitEnabled,
        String makeModel,
        String manufacturer,
        BigDecimal maxSpeed,
        Integer numCore,
        String speedUnit,
        LocalDateTime createDate,
        LocalDateTime changeDate
) {
}
