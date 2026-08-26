package com.itmsg.device42.dto.device42;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record Device42DeployedAssetSource(
        Integer devicePk,
        String name,
        String type,
        String notes,
        String serialNo,
        String assetNo,
        String uuid,
        Boolean networkDevice,
        String physicalSubtype,
        String hardwareName,
        String vendorName,
        Boolean inService,
        LocalDateTime lastDiscovered,
        BigDecimal ram,
        String ramSizeType,
        Integer totalCpus,
        Integer corePerCpu,
        String biosVersion,
        LocalDate biosReleaseDate
) {
}
