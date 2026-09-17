package com.itmsg.device42.maximo.ci.definition;

public record SpecDefinition(
        Long classSpecId, String classStructureId, String assetAttrId,
        String dataType, String section, String measureUnitId,
        int displaySequence, boolean mandatory,
        String linkedToAttribute, String linkedToSection
) {
}
