package com.itmsg.device42.dto.device42;

public record Device42DpaTcpIpSource(
        Long ipAddressPk,
        Long deviceFk,
        String deviceName,
        String ipAddress,
        String gateway,
        Integer maskBits
) {
}
