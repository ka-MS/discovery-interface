package com.itmsg.device42.integration.d42maximo.asset.tcpip;

/** view_ipaddress_v2 한 건과 device_fks에서 펼친 장비 연결 한 건. */
public record IpAddressSource(
        Long ipAddressPk,
        Long deviceFk,
        String deviceName,
        String ipAddress,
        String ipHybrid,
        String label,
        Long subnetFk,
        Long typeId,
        String type,
        Boolean available,
        Boolean publicAddress,
        Long resourceFk,
        String notes,
        String firstAdded,
        String lastEdited,
        String tags,
        Long netportFk,
        String details,
        String lastChanged,
        String lastDiscovered,
        Boolean shared,
        Long cloudInfrastructureFk,
        String gateway,
        Integer maskBits
) {
}
