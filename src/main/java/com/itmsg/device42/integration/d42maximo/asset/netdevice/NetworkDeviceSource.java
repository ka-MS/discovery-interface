package com.itmsg.device42.integration.d42maximo.asset.netdevice;

public record NetworkDeviceSource(
        Integer devicePk,
        String osVersion,
        String macAddress,
        String networkAddress
) {
}
