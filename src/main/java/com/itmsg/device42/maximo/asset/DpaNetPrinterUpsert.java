package com.itmsg.device42.maximo.asset;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DpaNetPrinterUpsert(
        Long nodeId,
        BigDecimal currentRam,
        String netMacAddress,
        String networkAddress,
        Integer trayCount,
        String ramUnit,
        LocalDateTime createDate,
        LocalDateTime changeDate
) {
}
