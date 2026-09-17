package com.itmsg.device42.integration.d42maximo.asset.mediaadapter;

public record MediaAdapterSource(
        Long partPk,
        Long deviceFk,
        String serialNo,
        String description,
        String modelName,
        String modelDescription,
        String vendorName
) {
}
