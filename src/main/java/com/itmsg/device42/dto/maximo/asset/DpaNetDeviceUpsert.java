package com.itmsg.device42.dto.maximo.asset;

import java.time.LocalDateTime;

public record DpaNetDeviceUpsert(
        Long nodeId,
        String netMacAddress,
        String networkAddress,
        String osVersion,
        LocalDateTime createDate,
        LocalDateTime changeDate
) {
}
