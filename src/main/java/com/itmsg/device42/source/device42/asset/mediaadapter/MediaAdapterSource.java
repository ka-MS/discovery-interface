package com.itmsg.device42.source.device42.asset.mediaadapter;

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
