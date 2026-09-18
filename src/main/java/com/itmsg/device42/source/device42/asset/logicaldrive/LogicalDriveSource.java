package com.itmsg.device42.source.device42.asset.logicaldrive;

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
