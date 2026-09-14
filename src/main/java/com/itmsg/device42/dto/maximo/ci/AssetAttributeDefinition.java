package com.itmsg.device42.dto.maximo.ci;

public record AssetAttributeDefinition(
        long id, String attributeId, String dataType, String measureUnitId,
        String orgId, String siteId
) {
}
