package com.itmsg.device42.integration.d42maximo.asset.computer;

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
