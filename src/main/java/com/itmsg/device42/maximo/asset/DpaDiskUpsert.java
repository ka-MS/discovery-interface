package com.itmsg.device42.maximo.asset;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DpaDiskUpsert(
        Long diskId,
        Long nodeId,
        String description,
        String diskInterface,
        Integer externalDevice,
        Integer hotSwappable,
        String makeModel,
        String manufacturer,
        Integer removableMedia,
        String serialNumber,
        String sizeUnit,
        BigDecimal totalSpace,
        Integer writeCapable,
        LocalDateTime createDate,
        LocalDateTime changeDate
) {
}
