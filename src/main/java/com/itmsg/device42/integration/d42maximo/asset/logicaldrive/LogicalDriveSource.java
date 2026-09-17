package com.itmsg.device42.integration.d42maximo.asset.logicaldrive;

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
