package com.itmsg.device42.integration.d42maximo.asset.netprinter;

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
