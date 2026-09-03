package com.itmsg.device42.dto.maximo.software;

public record TloamSoftwareUpsert(
        String uniqueId,
        String softwareName,
        String manufacturer,
        String version
) {
}
