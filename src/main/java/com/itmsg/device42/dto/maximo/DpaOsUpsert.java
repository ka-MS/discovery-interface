package com.itmsg.device42.dto.maximo;

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
