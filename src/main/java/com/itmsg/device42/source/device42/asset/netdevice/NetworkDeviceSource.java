package com.itmsg.device42.source.device42.asset.netdevice;

public record NetworkDeviceSource(
        Integer devicePk,
        String osVersion,
        String macAddress,
        String networkAddress
) {
}
