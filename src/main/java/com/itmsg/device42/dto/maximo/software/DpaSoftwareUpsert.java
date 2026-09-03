package com.itmsg.device42.dto.maximo.software;

import java.time.LocalDateTime;

public record DpaSoftwareUpsert(
        Long softwareId,
        Long nodeId,
        String softwareName,
        String manufacturer,
        String version,
        String tloamUniqueId,
        String installPath,
        LocalDateTime installDate,
        LocalDateTime firstEncountered,
        LocalDateTime lastEncountered,
        Long suiteId,
        LocalDateTime createDate,
        LocalDateTime changeDate
) {
}
