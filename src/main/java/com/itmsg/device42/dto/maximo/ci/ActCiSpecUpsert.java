package com.itmsg.device42.dto.maximo.ci;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ActCiSpecUpsert(
        String actCiNum, long refObjectId, String classStructureId,
        String assetAttrId, long classSpecId, String section,
        int displaySequence, boolean mandatory, String measureUnitId,
        String linkedToAttribute, String linkedToSection,
        String alnValue, BigDecimal numValue,
        String changeBy, LocalDateTime changeDate
) {
}
