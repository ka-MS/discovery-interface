package com.itmsg.device42.dto.device42.asset;

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
