package com.itmsg.device42.maximo.ci;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ActCiSpecUpsert(
        String actCiNum, String classStructureId,
        String assetAttrId, Long classSpecId, String section,
        int displaySequence, boolean mandatory, String measureUnitId,
        String linkedToAttribute, String linkedToSection,
        String alnValue, BigDecimal numValue,
        String changeBy, LocalDateTime changeDate
) {
}
