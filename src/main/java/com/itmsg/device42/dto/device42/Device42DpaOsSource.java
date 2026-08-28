package com.itmsg.device42.dto.device42;

public record Device42DpaOsSource(
        Long deviceosPk,
        Long deviceFk,
        String osName,
        String osVersion,
        String osVersionNo,
        String vendorName
) {
}
