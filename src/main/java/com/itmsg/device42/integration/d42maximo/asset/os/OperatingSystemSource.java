package com.itmsg.device42.integration.d42maximo.asset.os;

public record OperatingSystemSource(
        Long deviceosPk,
        Long deviceFk,
        String osName,
        String osVersion,
        String osVersionNo,
        String vendorName
) {
}
