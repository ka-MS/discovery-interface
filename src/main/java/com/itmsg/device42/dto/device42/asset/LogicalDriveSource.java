package com.itmsg.device42.dto.device42.asset;

import java.math.BigDecimal;

public record LogicalDriveSource(
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
