package com.itmsg.device42.dto.maximo.asset;

import java.time.LocalDateTime;

public record DpaTcpIpUpsert(
        Long tcpIpId,
        Long nodeId,
        String gateway,
        String host,
        String tcpIpAddress,
        String tcpIpNetmask,
        LocalDateTime createDate,
        LocalDateTime changeDate
) {
}
