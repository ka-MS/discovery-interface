package com.itmsg.device42.dto.device42;

import java.time.LocalDateTime;

public record Device42DpaSoftwareSource(
        Long softwareInUsePk,
        Long deviceFk,
        String softwareName,
        String version,
        String installPath,
        LocalDateTime installDate,
        LocalDateTime firstDetected,
        LocalDateTime lastUpdated,
        String vendorName
) {
}
