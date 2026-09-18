package com.itmsg.device42.target.maximo.ci.definition;

public record SpecDefinition(
        Long classSpecId, String classStructureId, String assetAttrId,
        String dataType, String section, String measureUnitId,
        int displaySequence, boolean mandatory,
        String linkedToAttribute, String linkedToSection
) {
}
