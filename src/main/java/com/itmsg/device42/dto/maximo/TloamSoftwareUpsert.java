package com.itmsg.device42.dto.maximo;

public record TloamSoftwareUpsert(
        String uniqueId,
        String softwareName,
        String manufacturer,
        String version
) {
}
