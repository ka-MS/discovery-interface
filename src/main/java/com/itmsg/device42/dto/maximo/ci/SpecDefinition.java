package com.itmsg.device42.dto.maximo.ci;

public record SpecDefinition(
        long classSpecId, String classStructureId, String assetAttrId,
        String dataType, String section, String measureUnitId,
        int displaySequence, boolean mandatory,
        String linkedToAttribute, String linkedToSection
) {
}
