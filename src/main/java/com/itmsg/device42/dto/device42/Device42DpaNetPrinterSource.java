package com.itmsg.device42.dto.device42;

import java.math.BigDecimal;

public record Device42DpaNetPrinterSource(
        Integer devicePk,
        BigDecimal currentRam,
        String ramUnit,
        String macAddress,
        String networkAddress,
        Integer trayCount
) {
}
