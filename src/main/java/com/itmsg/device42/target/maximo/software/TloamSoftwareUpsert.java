package com.itmsg.device42.target.maximo.software;

public record TloamSoftwareUpsert(
        String uniqueId,
        String softwareName,
        String manufacturer,
        String version
) {
}
