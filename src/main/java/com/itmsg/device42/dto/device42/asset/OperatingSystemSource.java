package com.itmsg.device42.dto.device42.asset;

public record OperatingSystemSource(
        Long deviceosPk,
        Long deviceFk,
        String osName,
        String osVersion,
        String osVersionNo,
        String vendorName
) {
}
