package com.itmsg.device42.dto.device42;

import java.math.BigDecimal;

public record Device42DpaComputerSource(
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
