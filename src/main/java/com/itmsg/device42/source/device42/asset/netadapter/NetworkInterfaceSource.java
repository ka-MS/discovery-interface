package com.itmsg.device42.source.device42.asset.netadapter;

public record NetworkInterfaceSource(
        Long netportPk,
        Long deviceFk,
        String port,
        String description,
        String hwaddress,
        String hwaddress2,
        String portSpeed,
        String globalType,
        String vendorName
) {
}
