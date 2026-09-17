package com.itmsg.device42.integration.d42maximo.asset.netadapter;

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
