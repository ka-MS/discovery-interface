package com.itmsg.device42.dto.maximo.asset;

import java.time.LocalDateTime;

public record DpaMediaAdapterUpsert(
        Long adapterId,
        Long nodeId,
        String description,
        String makeModel,
        String manufacturer,
        String mediaType,
        String serialNumber,
        LocalDateTime createDate,
        LocalDateTime changeDate
) {
}
