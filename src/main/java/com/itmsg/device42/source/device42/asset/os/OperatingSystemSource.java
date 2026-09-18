package com.itmsg.device42.source.device42.asset.os;

public record OperatingSystemSource(
        Long deviceosPk,
        Long deviceFk,
        String osName,
        String osVersion,
        String osVersionNo,
        String vendorName
) {
}
