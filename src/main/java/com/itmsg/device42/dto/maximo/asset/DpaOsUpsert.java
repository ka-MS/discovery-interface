package com.itmsg.device42.dto.maximo.asset;

import java.time.LocalDateTime;

public record DpaOsUpsert(
        Long osId,
        Long nodeId,
        String build,
        String manufacturer,
        String name,
        String version,
        LocalDateTime createDate,
        LocalDateTime changeDate
) {
}
