package com.itmsg.device42.dto.device42.asset;

import java.math.BigDecimal;

public record NetworkPrinterSource(
        Integer devicePk,
        BigDecimal currentRam,
        String ramUnit,
        String macAddress,
        String networkAddress,
        Integer trayCount
) {
}
