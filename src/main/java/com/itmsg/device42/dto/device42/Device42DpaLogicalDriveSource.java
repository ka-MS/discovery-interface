package com.itmsg.device42.dto.device42;

import java.math.BigDecimal;

public record Device42DpaLogicalDriveSource(
        Long mountPointPk,
        Long deviceFk,
        String mountPoint,
        String fileSystem,
        String fileSystemType,
        BigDecimal totalSize,
        BigDecimal availableSize,
        String volumeLabel
) {
}
