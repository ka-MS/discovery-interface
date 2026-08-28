package com.itmsg.device42.integration.maximo;

public record SourceTargetMapKey(
        String sourceSystem,
        String sourceObject,
        Long sourceId,
        String targetSystem,
        String targetObject
) {
}
