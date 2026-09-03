package com.itmsg.device42.dto.device42.asset;

public record NetworkDeviceSource(
        Integer devicePk,
        String osVersion,
        String macAddress,
        String networkAddress
) {
}
