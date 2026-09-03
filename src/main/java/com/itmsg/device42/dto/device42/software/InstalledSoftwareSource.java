package com.itmsg.device42.dto.device42.software;

import java.time.LocalDateTime;

public record InstalledSoftwareSource(
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
