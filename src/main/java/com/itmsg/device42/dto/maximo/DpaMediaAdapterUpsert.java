package com.itmsg.device42.dto.maximo;

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
