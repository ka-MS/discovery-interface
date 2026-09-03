package com.itmsg.device42.dto.device42.asset;

import java.math.BigDecimal;

public record ComputerHardwareSource(
        Integer devicePk,
        String biosName,
        String biosVersion,
        String biosReleaseDate,
        BigDecimal ram,
        String ramSizeType,
        Integer totalCpus,
        Integer corePerCpu
) {
}
