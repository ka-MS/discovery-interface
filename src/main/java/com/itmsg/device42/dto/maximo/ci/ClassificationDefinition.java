package com.itmsg.device42.dto.maximo.ci;

import java.util.Map;

public record ClassificationDefinition(
        String classificationId, String classStructureId,
        Map<String, SpecDefinition> specs
) {
    public ClassificationDefinition {
        specs = Map.copyOf(specs);
    }
}
