package com.itmsg.device42.target.maximo.asset;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DpaLogicalDriveUpsert(
        Long logicalDriveId,
        Long nodeId,
        String attachedNetworkName,
        BigDecimal availableSize,
        Integer compressed,
        String driveType,
        Integer encrypted,
        String fileSystem,
        String mount,
        String sizeUnit,
        BigDecimal totalSize,
        String volumeLabel,
        LocalDateTime createDate,
        LocalDateTime changeDate
) {
}
