package com.itmsg.device42.dto.device42.asset;

public record IpAddressSource(
        Long ipAddressPk,
        Long deviceFk,
        String deviceName,
        String ipAddress,
        String gateway,
        Integer maskBits
) {
}
