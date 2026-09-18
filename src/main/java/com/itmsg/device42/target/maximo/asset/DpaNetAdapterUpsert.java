package com.itmsg.device42.target.maximo.asset;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DpaNetAdapterUpsert(
        Long adapterId,
        Long nodeId,
        String adapterType,
        BigDecimal bandwidth,
        String bandwidthUnit,
        String description,
        String makeModel,
        String manufacturer,
        String netMacAddr1,
        String netMacAddr2,
        String port,
        String protocol,
        LocalDateTime createDate,
        LocalDateTime changeDate
) {
}
