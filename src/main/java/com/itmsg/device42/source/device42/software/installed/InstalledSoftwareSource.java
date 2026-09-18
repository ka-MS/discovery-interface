package com.itmsg.device42.source.device42.software.installed;

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
