package com.itmsg.device42.dto.device42;

public record Device42DpaMediaAdapterSource(
        Long partPk,
        Long deviceFk,
        String serialNo,
        String description,
        String modelName,
        String modelDescription,
        String vendorName
) {
}
