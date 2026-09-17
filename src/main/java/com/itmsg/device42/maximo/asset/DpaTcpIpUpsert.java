package com.itmsg.device42.maximo.asset;

import java.time.LocalDateTime;

/** TCPIPID는 신규 INSERT에서 MAXIMO.DPATCPIPSEQ로 채번한다. */
public record DpaTcpIpUpsert(
        Long nodeId,
        String gateway,
        String host,
        String tcpIpAddress,
        String tcpIpNetmask,
        LocalDateTime createDate,
        LocalDateTime changeDate
) {
}
