package com.itmsg.device42.dto.device42;

public record Device42DpaNetAdapterSource(
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
