package com.itmsg.device42.dto.device42;

public record Device42DpaNetDeviceSource(
        Integer devicePk,
        String osVersion,
        String macAddress,
        String networkAddress
) {
}
